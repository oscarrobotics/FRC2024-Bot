package frc.robot.Subsystems;

import frc.robot.Constants;
import frc.robot.PhotonCameraWrapper;
import frc.robot.Constants.ShooterK;

import static edu.wpi.first.units.Units.Rotation;
import static edu.wpi.first.units.Units.Rotations;

import java.util.Optional;
import java.util.function.Supplier;

import javax.swing.text.Position;

import org.photonvision.EstimatedRobotPose;
import org.photonvision.PhotonCamera;

import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.revrobotics.AbsoluteEncoder;
import com.revrobotics.CANSparkFlex;
import com.revrobotics.SparkAbsoluteEncoder;
import com.revrobotics.SparkFlexExternalEncoder;
import com.revrobotics.CANSparkLowLevel.MotorType;

import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.units.Angle;
import edu.wpi.first.units.Measure;
import edu.wpi.first.units.Velocity;
import edu.wpi.first.wpilibj.AnalogPotentiometer;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.ProfiledPIDCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Shooter extends SubsystemBase{
    //2 NEO Vortex Flexes for Shooter
    //Absolute Encoder
    private Timer m_Timer;

    private AnalogPotentiometer m_pivPotentiometer;
    private double positionDeg;

    private DigitalInput m_shootBeamBreaker;

    private CANSparkFlex m_leftShootMotor, m_rightShootMotor;
    private TalonFX m_leftSledPivotMotor, m_rightSledPivotMotor, m_shootPivotMotor;

    private DutyCycleEncoder m_absoluteEncoder;

    private boolean isStowed;

    private TrapezoidProfile.Constraints m_Tiltconstraints = new TrapezoidProfile.Constraints(1,1);

    private ProfiledPIDController m_Tiltcontroller = 
        new ProfiledPIDController(0, 0, 0, m_Tiltconstraints, 0.02); 

    private TrapezoidProfile.Constraints m_Sledconstraints = new TrapezoidProfile.Constraints(1,1);

    private ProfiledPIDController m_Sledcontroller = 
        new ProfiledPIDController(0, 0, 0, m_Sledconstraints, 0.02); 
    
    private DutyCycleOut motorRequest = new DutyCycleOut(0.0); 

    private final PositionVoltage m_request = new PositionVoltage(0);

    

    public Shooter(){
        m_leftShootMotor = new CANSparkFlex(Constants.kLeftShootMotorId, MotorType.kBrushless);
        m_rightShootMotor = new CANSparkFlex(Constants.kRightShootMotorId, MotorType.kBrushless);

        m_leftSledPivotMotor = new TalonFX(Constants.kLeftSledPivotId);
        m_rightSledPivotMotor = new TalonFX(Constants.kRightSledPivotId);

        // m_sledPivotMotor = new TalonFX(1);
        m_shootPivotMotor = new TalonFX(Constants.kShootPivotId, "rio");

        m_absoluteEncoder = new DutyCycleEncoder(2);
        // m_absoluteEncoder.setDistancePerRotation(positionDeg);

        m_pivPotentiometer = new AnalogPotentiometer(1, 300, -11.9);

        m_shootBeamBreaker = new DigitalInput(1);

        m_leftSledPivotMotor.setControl(new Follower(m_rightSledPivotMotor.getDeviceID(), true));
    }

    //debugging --> encoder output, control the motor, PID values

    /* States */
    public boolean isInSled(){
        isStowed = (!m_shootBeamBreaker.get())?true:false;
        return isStowed;
    }

    /* Velocity */
      public Command toWheelSpeeds(double velocity){
        // frontWheelTargetSpeed = velocity.in(Rotations.per(Minute));
        return runEnd(() -> {
            m_leftShootMotor.set(velocity);
            m_rightShootMotor.set(velocity);
        }, () -> {
            m_leftShootMotor.set(0);
            m_leftShootMotor.set(0);
        });
    }
    /* Sled Pivot */
    public double getPivotVoltage(){
        return m_pivPotentiometer.get();
    }

     //actual angle = (actual voltage – 0deg voltage) * degrees_per_volt -> y = mx + b
    public double getSledPivotAngle(){
        return (m_pivPotentiometer.get());
        //min = 0 with -11.9 ofset
        //max = 56 with -11.9 ofset
    }

    /*Shoot Pivot */ // -> for amp

    public double getPivotAbsPosition(){
        return m_absoluteEncoder.getAbsolutePosition();
    }

    public double getShootPivotAngle(){
        return m_absoluteEncoder.get();
        //straight valure = 0.487
        //maxtiltforward = 0.873
        //maxtiltbackword = 0.177
    }

    public void setTargetTilt(double angle){
        m_Tiltcontroller.setGoal(angle);
    }

    public boolean get_beam(){
        return m_shootBeamBreaker.get();
    }

    /* Commands */

    //isInShooter

    //isAlignedWithFeed

    //TODO: Change to RunEnd cmd(?); Add PID


    public Command toTargetAngle(Measure<Angle> angle){
        return runOnce(() -> {
            m_rightSledPivotMotor.setControl(m_request.withPosition(angle.in(Rotations)));
            // toWheelSpeeds(pos); 
        });
    }

    public Command shootNote(){
        //if aligned, will shoot
        return runOnce(() -> {
            toWheelSpeeds(7000);
        }).until(() -> m_Timer.hasElapsed(1));
    }
    //shooter: shoot the note out

    @Override
    public void periodic(){
        // m_shootPivotMotor.setControl(motorRequest.withOutput(m_controller.calculate(m_absoluteEncoder.get())));
    }
}
