package ru.goodibunakov.pinview;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public class SavedState extends View.BaseSavedState {

    private String entered;

    SavedState(Parcelable superState) {
        super(superState);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(entered);
    }

    public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
        public SavedState createFromParcel(Parcel in) {
            return new SavedState(in);
        }

        public SavedState[] newArray(int size) {
            return new SavedState[size];
        }
    };

    private SavedState(Parcel in) {
        super(in);
        entered = in.readString();
    }

    public void setEntered(String entered) {
        this.entered = entered;
    }

    public String getEntered() {
        return entered;
    }
}
