package org.firstinspires.ftc.teamcode;

import android.util.Log;
import android.widget.Toast;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.Range;

//import org.firstinspires.ftc.robotcore.internal.AppUtil;
//import org.firstinspires.ftc.robotcore.internal.UILocation;

@TeleOp(name = "Go Baby Go!")
public class GoBabyGo extends OpMode {
    DcMotor driveRelay;     // was leftDrive in direct drive version
    DcMotor reverseRelay;   // was rightDrive in direct drive version
    DcMotor fastRelay;      // Slow/fast speed relay
    DcMotor steer;

    TouchSensor leftSteerStop;
    TouchSensor rightSteerStop;

    AnalogInput xAxis;
    AnalogInput yAxis;
    double xMax;
    double yMax;

    boolean active = false;
    boolean override = true;
    boolean speedBoost = false;

    private boolean localControl = false;
    private boolean remoteControl = false;
    private boolean goingFast = false;
    private boolean goingSlow = false;

    private double speedLimit = 1.0;
    private long accelTime = 1000;   // 0-speedLimit acceleration time, mSec
    private double steerMax = 0.3;
    private int fwdRevDelayCount = 200; // Coast this many loop()s when reversing direction

    private boolean drivingFwd = false;
    private boolean drivingRev = false;
    private int fwdRevDelay = 0;

    public void init() {
        telemetry.addLine("Press \"Start\" + \"A\" to activate.");
        telemetry.update();

        driveRelay = hardwareMap.dcMotor.get("drive");
        reverseRelay = hardwareMap.dcMotor.get("reverse");
        fastRelay = hardwareMap.dcMotor.get("speed");
        steer = hardwareMap.dcMotor.get("steer");

        leftSteerStop = hardwareMap.touchSensor.get("lT");
        rightSteerStop = hardwareMap.touchSensor.get("rT");

        xAxis = hardwareMap.analogInput.get("x");
        yAxis = hardwareMap.analogInput.get("y");
        xMax = xAxis.getMaxVoltage();
        yMax = yAxis.getMaxVoltage();

        driveRelay.setPower(0.0);
        reverseRelay.setPower(0.0);
        fastRelay.setPower(0.0);

        drivingFwd = false;
        drivingRev = false;
        fwdRevDelay = 0;

        active = true;
    }

    public void loop() {
        double speed;
        double steer;

        if (override) {
            speed = -gamepad1.right_stick_y;
            steer = -gamepad1.right_stick_x;

            if (!remoteControl) {
                telemetry.addLine("User Override!\nPress \"A\" to give control to Henry");
                if (goingFast)
                    telemetry.addLine("Going fast!\nPress \"X\" to go slow");
                else
                    telemetry.addLine("Going slow!\nPress \"Y\" to go fast");
                telemetry.update();
                remoteControl = true;
                localControl = false;
            }

            if (gamepad1.a) {
                override = false;
            }
        } else {
            speed = Range.scale(xAxis.getVoltage() / xMax, 1.0, 0.0, 1.0, -1.0);
            steer = Range.scale(yAxis.getVoltage() / yMax, 0.0, 1.0, -steerMax, steerMax);
            if (!localControl) {
                telemetry.addLine("Henry's in control!\nPress \"B\" to take control");
                if (goingFast)
                    telemetry.addLine("Going fast!\nPress \"X\" to go slow");
                else
                    telemetry.addLine("Going slow!\nPress \"Y\" to go fast");
                telemetry.update();
                localControl = true;
                remoteControl = false;
            }

            if (gamepad1.b) {
                override = true;
            }
        }

        // "X" button to go slow, "Y" button to go fast
        if (gamepad1.x)
        {
            if (override)
                telemetry.addLine("User Override!\nPress \"A\" to give control to Henry");
            else
                telemetry.addLine("Henry's in control!\nPress \"B\" to take control");
            telemetry.addLine("Going slow!\nPress \"Y\" to go fast");
            telemetry.update();

            goingSlow = true;
            goingFast = false;
            speedBoost = false;
        }
        if (gamepad1.y)
        {
            if (override)
                telemetry.addLine("User Override!\nPress \"A\" to give control to Henry");
            else
                telemetry.addLine("Henry's in control!\nPress \"B\" to take control");
            telemetry.addLine("Going fast!\nPress \"X\" to go slow");
            telemetry.update();

            goingSlow = false;
            goingFast = true;
            speedBoost = true;
        }

        if (speed > 0.5) {
            // If we were driving in reverse, coast a bit before driving forward
            if (drivingRev && (fwdRevDelay < fwdRevDelayCount)) {
                fwdRevDelay++;
                driveRelay.setPower(0.0);
            } else {
                drivingFwd = true;
                drivingRev = false;
                fwdRevDelay = 0;
                driveRelay.setPower(1.0);
            }
            reverseRelay.setPower(0.0); // not reverse
            fastRelay.setPower(speedBoost ? 1.0 : 0.0);
        } else if (speed < -0.5) {
            // If we were driving forward, coast a bit before driving in reverse
            if (drivingFwd && (fwdRevDelay < fwdRevDelayCount)) {
                fwdRevDelay++;
                driveRelay.setPower(0.0);
            } else {
                drivingRev = true;
                drivingFwd = false;
                fwdRevDelay = 0;
                driveRelay.setPower(1.0);
            }
            reverseRelay.setPower(1.0); // reverse
            fastRelay.setPower(speedBoost ? 1.0 : 0.0);
        } else {
            drivingRev = false;
            drivingFwd = false;
            fwdRevDelay = 0;
            reverseRelay.setPower(0.0); // not reverse
            driveRelay.setPower(0.0);   // not powered
            fastRelay.setPower(speedBoost ? 1.0 : 0.0);
        }

        if (rightSteerStop.isPressed()) {
            steer = Range.clip(steer, -steerMax, 0.0);
        } else if (leftSteerStop.isPressed()) {
            steer = Range.clip(steer, 0.0, steerMax);
        } else if (leftSteerStop.isPressed() && rightSteerStop.isPressed()) {
            steer = 0.0;
        } else {
            steer = Range.clip(steer, -steerMax, steerMax);
        }

        this.steer.setPower(steer);
    }

    public void stop() {
        active = false;
    }
}
