package com.example.foregroundservice

import android.os.Parcel
import android.os.Parcelable

/**
 * Scooter Speed
 */
class WheelData : Parcelable {

    var timestamp: Long
    var wheelSpeed: Int

    constructor() {
        this.timestamp = 0L
        this.wheelSpeed = 0
    }

    constructor(timestamp: Long, wheelSpeed: Int) {
        this.timestamp = timestamp
        this.wheelSpeed = wheelSpeed
    }

    private constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(timestamp)
        parcel.writeInt(wheelSpeed)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WheelData> {
        override fun createFromParcel(parcel: Parcel): WheelData {
            return WheelData(parcel)
        }

        override fun newArray(size: Int): Array<WheelData?> {
            return arrayOfNulls(size)
        }
    }

    override fun toString(): String {
        return "WheelData(timestamp=$timestamp, wheelSpeed=$wheelSpeed)"
    }
}