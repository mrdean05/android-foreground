package com.example.foregroundservice

import android.os.Parcel
import android.os.Parcelable

data class LocationData(
    var timestamp: Long,
    var longitude: Float,
    var latitude: Float,
    var attitude: Float,
    var direction: Float,
    var speed: Float,
    var hdop: Float
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(timestamp)
        parcel.writeFloat(longitude)
        parcel.writeFloat(latitude)
        parcel.writeFloat(attitude)
        parcel.writeFloat(direction)
        parcel.writeFloat(speed)
        parcel.writeFloat(hdop)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LocationData> {
        override fun createFromParcel(parcel: Parcel): LocationData {
            return LocationData(parcel)
        }

        override fun newArray(size: Int): Array<LocationData?> {
            return arrayOfNulls(size)
        }
    }

    override fun toString(): String {
        return "LocationData(timestamp=$timestamp, longitude=$longitude, latitude=$latitude, attitude=$attitude, direction=$direction, speed=$speed, hdop=$hdop)"
    }
}