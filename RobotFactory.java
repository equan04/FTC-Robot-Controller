package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.HardwareMap;


public class RobotFactory {
    static RobotHardware  theRobot = null;

    static public RobotHardware getRobotHardware(HardwareMap hardwareMap,RobotProfile robotProfile){
        if(theRobot == null){
            Logger.logFile("Creating new RobotHardware Instance");
            theRobot = new RobotHardware();
            theRobot.init(hardwareMap, robotProfile);
        }
        else {
            Logger.logFile("Use existing RobotHardware Instance");
        }
        return theRobot;
    }

    public static void reset() {
        theRobot = null;
    }
}
