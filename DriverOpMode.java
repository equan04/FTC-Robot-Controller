package org.firstinspires.ftc.teamcode;

import android.content.SharedPreferences;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.io.File;


@TeleOp(name="Newton DriverOpMode", group="Main")
public class DriverOpMode extends OpMode {
    RobotHardware robotHardware;
    RobotNavigator navigator;
    RobotProfile robotProfile;


    Pose2d currPose;
    double fieldHeadingOffset;

    boolean fieldMode;
    int fieldModeSign = 1;  // RED side = 1, BLUE side = -1
    boolean dpadRightDown = false;
    boolean dpadLeftDown = false;
    boolean bAlreadyPressed = false;

    // DriveThru combos
    SequentialComboTask grabLift;
    RobotControl currentTask = null;
    RobotVision robotVision;
    @Override
    public void init() {
        try {
            robotProfile = RobotProfile.loadFromFile(new File("/sdcard/FIRST/profile.json"));
        } catch (Exception e) {
        }

        fieldMode = true; //robot starts in field orientation

        Logger.init();

        robotHardware = RobotFactory.getRobotHardware(hardwareMap,robotProfile);
        //robotHardware = new RobotHardware();
        //robotHardware.init(hardwareMap, robotProfile);
        robotHardware.initRobotVision();
        robotVision = robotHardware.getRobotVision();
        robotVision.activateNavigationTarget();

        // Based on the Autonomous mode starting position, define the heading offset for field mode
        SharedPreferences prefs = AutonomousOptions.getSharedPrefs(hardwareMap);
        setupCombos();

    }

    private void handleVision() {
        if(gamepad1.x){
            Pose2d currentPosition = robotVision.getNavigationLocalization();
            if(currentPosition!=null){
                robotHardware.getTrackingWheelLocalizer().setPoseEstimate(currentPosition);
            }
        }
    }

    @Override
    public void loop() {
        robotHardware.getBulkData1();
        robotHardware.getBulkData2();
        robotHardware.getTrackingWheelLocalizer().update();
        currPose = robotHardware.getTrackingWheelLocalizer().getPoseEstimate();

        // Robot movement
        handleMovement();
        handleVision();

        // toggle field mode on/off.
        // Driver 1: left trigger - enable; right trigger - disable
        if (gamepad1.left_trigger > 0) {
            fieldMode = true;
            fieldHeadingOffset = currPose.getHeading();
        } else if (gamepad1.right_trigger > 0) {
            fieldMode = false;  //good luck driving
        }

        // test controls
        if (gamepad1.dpad_right && !dpadRightDown) {
            robotHardware.setArmNextPosition();
            dpadRightDown = true;
        }
        else {
            dpadRightDown = gamepad1.dpad_right;
        }
        if (gamepad1.dpad_left && !dpadLeftDown) {
            robotHardware.setArmPrevPosition();
            dpadLeftDown = true;
        }
        else {
            dpadLeftDown = gamepad1.dpad_left;
        }
        if (gamepad1.b) {
            robotHardware.setGrabberPosition(true);
        }
        if (gamepad1.a) {
            //robotHardware.setGrabberPosition(true);
            currentTask = grabLift;
            grabLift.prepare();
        }

        if (currentTask != null) {
            currentTask.execute();

            if (currentTask.isDone()) {
                currentTask.cleanUp();
                currentTask = null;
            }
        }
/*
        if(gamepad1.b && !bAlreadyPressed){
            bAlreadyPressed = true;
            Pose2d shootingPose = new Pose2d(-5, -36, Math.toRadians(0));
            double shootingHeading = 0;
            Trajectory trajectory = robotHardware.getMecanumDrive().trajectoryBuilder(currPose, false).
                    splineToSplineHeading(shootingPose, 0).build();
            currentTask = new SplineMoveTask(robotHardware.getMecanumDrive(), trajectory);
            currentTask.prepare();
        } else if(!gamepad1.b && bAlreadyPressed) {
            bAlreadyPressed = false;
        }
 */
        telemetry.addData("CurrPose", currPose);
        telemetry.addData("Field Mode", fieldMode);
    }

    @Override
    public void stop() {
        // open the clamp to relief the grabber servo
        try {
            Logger.logFile("DriverOpMode stop() called");
            robotVision.deactivateNavigationTarget();
            Logger.flushToFile();
        } catch (Exception e) {
        }
    }

    private void handleMovement() {
        if(currentTask instanceof SplineMoveTask){
            return;
        }
        double turn = gamepad1.right_stick_x/2;
        double power = Math.hypot(gamepad1.left_stick_x, -gamepad1.left_stick_y);
        double moveAngle = Math.atan2(-gamepad1.left_stick_y, gamepad1.left_stick_x) - Math.PI/4;

        if (fieldMode) {
            moveAngle += currPose.getHeading() - fieldHeadingOffset + fieldModeSign*Math.PI/2;
        }

        power = power / 1.5;
        turn = turn / 4;
        if (gamepad1.left_bumper) { // further bring it down
            power = power/2;
            turn = turn/3;
        }
        /** what is this?!!
        if(gamepad1.x && !xAlreadyPressed){
            xAlreadyPressed = true;
            power = power/3.5;
            turn = turn/13;
        } else if(gamepad1.x && xAlreadyPressed){
            xAlreadyPressed = false;
            power = Math.hypot(gamepad1.left_stick_x, -gamepad1.left_stick_y);
            turn = gamepad1.right_stick_x;
        }
        */

        robotHardware.mecanumDriveTest(power, moveAngle, turn, 0);
    }

    void setupCombos() {
        grabLift = new SequentialComboTask();
        grabLift.addTask(new GrabberTask(robotHardware, robotProfile, false, 500));
        grabLift.addTask(new MoveArmTask(robotHardware, robotProfile, RobotHardware.ArmPosition.HOLD, 500));
//        ArrayList<RobotControl> homePositionList = new ArrayList<RobotControl>();
//        homePositionList.add(new SetLiftPositionTask(robotHardware, robotProfile, robotProfile.hardwareSpec.liftStoneBase +
//                robotProfile.hardwareSpec.liftPerStone + robotProfile.hardwareSpec.liftGrabExtra, 100));
//        homePositionList.add(new ClampStraightAngleTask(robotHardware, robotProfile));
//        homePositionList.add(new SetSliderPositionTask(robotHardware, robotProfile, robotProfile.hardwareSpec.sliderOrigPos, 100));
//        homePositionList.add(new ClampOpenCloseTask(robotHardware, robotProfile, RobotHardware.ClampPosition.OPEN));
//        homePositionList.add(new SetLiftPositionTask(robotHardware, robotProfile, robotProfile.hardwareSpec.liftStoneBase +
//                robotProfile.hardwareSpec.liftHomeReadyPos, 100));
//        homePositionTask = new SequentialComboTask();
//        homePositionTask.setTaskList(homePositionList);
    }


}
