package ru.goodibunakov.pinview

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Parcelable
import android.text.Editable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.min


class PinView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatEditText(context, attrs, defStyleAttr), OnFocusChangeListener {


    constructor(
        context: Context,
        callback: PinEnterCallback
    ) : this(context, attrs = null, defStyleAttr = 0) {
        this.callback = callback
    }

    interface PinEnterCallback {
        fun pinEnterFinished(pass: String)
        fun pinNotEntered()
    }

    private val SPACE = 100f
    private val RADIUS = 100f
    private val COUNT = 4
    var callback: PinEnterCallback? = null

    var colorInFocus = 0
    var colorEntered = 0
    var colorEmpty = 0
    var radius = 0f
    var countOfCircles = 0
    var spaceBetweenCircles = 1f
    var fitToScreen = false
    var dxStart = 0f
    private var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var enteredPassword = StringBuilder()

    private var imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        isLongClickable = false
        isCursorVisible = false
        inputType = InputType.TYPE_CLASS_NUMBER
        setRawInputType(InputType.TYPE_CLASS_NUMBER)

        context.withStyledAttributes(attrs, R.styleable.PinView) {
            colorInFocus = getColor(
                R.styleable.PinView_color_in_focus,
                ContextCompat.getColor(context, R.color.pinInFocus)
            )
            colorEntered = getColor(
                R.styleable.PinView_color_entered,
                ContextCompat.getColor(context, R.color.pinEntered)
            )
            colorEmpty = getColor(
                R.styleable.PinView_color_entered,
                ContextCompat.getColor(context, R.color.pinEmpty)
            )

            countOfCircles = getInt(R.styleable.PinView_countOfCircles, COUNT)
            if (countOfCircles < 4) countOfCircles = COUNT
            radius = getDimension(R.styleable.PinView_radius, RADIUS)
            spaceBetweenCircles = getDimension(R.styleable.PinView_spaceBetweenCircles, SPACE)
            fitToScreen = getBoolean(R.styleable.PinView_fitToScreen, false)
        }

        val maxLength = countOfCircles
        filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))


        var isBackspaceClicked = false
        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!isBackspaceClicked) {
                    enteredPassword.append(s.toString().last())
                    Log.d("debug", "key = $enteredPassword")
                    postInvalidate()
                    if (enteredPassword.length == countOfCircles) {
                        Toast.makeText(
                            context,
                            "Введен пароль $enteredPassword",
                            Toast.LENGTH_SHORT
                        ).show()
                        callback?.pinEnterFinished(enteredPassword.toString())
                    }
                } else {
                    callback?.pinNotEntered()
                    if (enteredPassword.isNotEmpty()) {
                        enteredPassword.deleteCharAt(enteredPassword.length - 1)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                isBackspaceClicked = after < count
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (hasFocus) {
            Log.d("debug", "has focus")
            showKeyboard()
        } else {
            Log.d("debug", "has NOT focus")
            hideSoftKeyboard()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val desiredWidth =
            (2 * radius * countOfCircles + spaceBetweenCircles * (countOfCircles - 1)).toInt()
        val desiredHeight = (radius * 2).toInt()

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val widthMeasured: Int
        var heightMeasured: Int

        //Measure Width
        widthMeasured = when (widthMode) {
            MeasureSpec.EXACTLY -> {
                //Должен быть именно такого размера
                widthSize
            }
            MeasureSpec.AT_MOST -> {
                //Не может быть больше чем...
                min(desiredWidth, widthSize)
            }
            else -> {
                //Может быть любого размера
                desiredWidth
            }
        }

        //Measure Height
        heightMeasured = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                //Must be this size
                heightSize
            }
            MeasureSpec.AT_MOST -> {
                //Can't be bigger than...
                min(desiredHeight, heightSize)
            }
            else -> {
                //Be whatever you want
                desiredHeight
            }
        }

        /**
         * Вычисления, если fitToScreen = true
         */
        if (fitToScreen) {
            if ((spaceBetweenCircles * (countOfCircles - 1) + 2 * radius * countOfCircles) > (widthMeasured - paddingLeft - paddingRight)) {
                val ratio = radius / spaceBetweenCircles
                spaceBetweenCircles =
                    (widthMeasured - paddingLeft - paddingRight) / (countOfCircles - 1 + 2 * ratio * countOfCircles)
                radius = spaceBetweenCircles * ratio
            }
            if (2 * radius < heightMeasured) heightMeasured = (2 * radius).toInt()
        }

        /**
         * Вычисляем начальный отступ слева и справа
         */
        if ((spaceBetweenCircles * (countOfCircles - 1) + 2 * radius * countOfCircles) < (widthMeasured - paddingLeft - paddingRight)) {
            dxStart =
                (widthMeasured - paddingLeft - paddingRight - (spaceBetweenCircles * (countOfCircles - 1) + 2 * radius * countOfCircles)) / 2
        }


        //Обязательный вызов в конце метода onMeasure
        setMeasuredDimension(widthMeasured, heightMeasured)
    }

    override fun onDraw(canvas: Canvas?) {
        var dx = dxStart
        /**
         * Отрисовка пинов в зависимости от количества пинов
         * и числа введенных символов
         */
        for (i in 1..countOfCircles) {
            if (enteredPassword.isEmpty() && !hasFocus()) {
                drawCircleNotInFocus(canvas, dx)
            }
            if (enteredPassword.isEmpty() && hasFocus()) {
                if (i == 1) {
                    drawCircleInFocus(canvas, dx)
                } else {
                    drawCircleNotInFocus(canvas, dx)
                }
            }
            if (enteredPassword.isNotEmpty()) {
                if (i <= enteredPassword.length) {
                    drawCircleEntered(canvas, dx)
                } else if (i == enteredPassword.length + 1) {
                    drawCircleInFocus(canvas, dx)
                } else {
                    drawCircleNotInFocus(canvas, dx)
                }
            }
            dx += radius * 2 + spaceBetweenCircles
        }
    }

    private fun drawCircleNotInFocus(canvas: Canvas?, dx: Float) {
        paint.style = Paint.Style.FILL
        paint.color = colorEmpty
        canvas?.drawCircle(radius + dx, height / 2.toFloat(), radius, paint)
    }

    private fun drawCircleInFocus(canvas: Canvas?, dx: Float) {
        paint.style = Paint.Style.FILL
        paint.color = colorEmpty
        canvas?.drawCircle(radius + dx, height / 2.toFloat(), radius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = radius / 8
        paint.color = colorInFocus
        canvas?.drawCircle(radius + dx, height / 2.toFloat(), radius - paint.strokeWidth / 2, paint)
    }

    private fun drawCircleEntered(canvas: Canvas?, dx: Float) {
        paint.style = Paint.Style.FILL
        paint.color = colorEmpty
        canvas?.drawCircle(radius + dx, height / 2.toFloat(), radius, paint)
        paint.color = colorEntered
        canvas?.drawCircle(radius + dx, height / 2.toFloat(), radius / 1.5f, paint)
    }

    private fun showKeyboard() {
        if (requestFocus()) {
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideSoftKeyboard() {
        imm.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val st = SavedState(super.onSaveInstanceState())
        st.entered = enteredPassword.toString()
        return st
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is ru.goodibunakov.pinview.SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        enteredPassword = StringBuilder(state.entered)
    }

    private fun createAnimator(): ValueAnimator {
        val animator = ValueAnimator.ofInt(0, 10)
        animator.duration = 2000
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            ValueAnimator.AnimatorUpdateListener {

            }
        }
        return animator
    }
}