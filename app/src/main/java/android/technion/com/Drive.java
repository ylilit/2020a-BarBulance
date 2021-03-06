package android.technion.com;

import android.net.Uri;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Drive implements Serializable {
    final String collection="Drives";

    String databaseID;
    String driverId;
    String driverProfilePicUri;
    String driverFullName;
    String driverPhoneNumber;
    String fromLocation;
    String fromCity;
    String toLocation;
    String toCity;
    String date;
    String time;
    String eventID;

    public Drive(String driverId, String driverProfilePicUri, String driverFullName,
                 String driverPhoneNumber, String fromLocation, String fromCity, String toLocation,
                 String toCity, String date, String time){

        this.databaseID = "";
        this.driverId = driverId;
        this.driverProfilePicUri = driverProfilePicUri;
        this.driverFullName = driverFullName;
        this.driverPhoneNumber = driverPhoneNumber;
        this.fromLocation = fromLocation;
        this.fromCity = fromCity;
        this.toLocation = toLocation;
        this.toCity = toCity;
        this.date = date;
        this.time = time;
    }
}
