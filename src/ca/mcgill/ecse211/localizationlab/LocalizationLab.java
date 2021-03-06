package ca.mcgill.ecse211.localizationlab;


import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.SensorModes;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;

	
public class LocalizationLab {

  public static final double WHEEL_RADIUS = 2.1;            //Wheel radius (cm)
  public static final double TRACK = 14.4;                  //Wheel base length (cm)
  
  //Setup right and left motors
  private static final EV3LargeRegulatedMotor leftMotor =   //Left motor uses port A
    new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
  private static final EV3LargeRegulatedMotor rightMotor =  //Right motor uses port D
    new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
  
  //Initialize ultrasonic sensor to port 2
  private static final Port usPort = LocalEV3.get().getPort("S2"); 
  private static final Port csPort = LocalEV3.get().getPort("S1");
  
  public static void main(String[] args) {
    int buttonChoice = 0;                                   //Contains the user input
    final TextLCD t = LocalEV3.get().getTextLCD();          //Initialize text field          
    Odometer odometer = new Odometer(leftMotor, rightMotor);//Initialize odometer
    OdometryDisplay odometryDisplay = new OdometryDisplay(odometer, t); //Initialize Odometer display
		                            
    //Create an instance of the US sensor, and a buffer to contain its data
    @SuppressWarnings("resource")
    SensorModes usSensor = new EV3UltrasonicSensor(usPort);
    SampleProvider usDistance = usSensor.getMode("Distance"); 
    float[] usData = new float[usDistance.sampleSize()];
    
    @SuppressWarnings("resource")
    EV3ColorSensor csSensor = new EV3ColorSensor(csPort);
    SampleProvider csColor = csSensor.getMode("Red");
    float[] csData = new float[csColor.sampleSize()];
    
    ColorSensorPoller csPoller = new ColorSensorPoller(csColor,csData);
    csPoller.start();
    
    OdometryCorrection odometryCorrection = new OdometryCorrection(odometer, csPoller);
    
    Navigation navigator = new Navigation(odometer, leftMotor, rightMotor, WHEEL_RADIUS, WHEEL_RADIUS, TRACK);
    
    float localizationScan[] = new float[50];
    
	//Create an instance of the US Poller, to take samples from the US sensor in a thread.	  
    UltrasonicPoller usPoller = new UltrasonicPoller(usDistance,usData);
    usPoller.start();
    
    final UltrasonicLocalizer usLocalizer = new UltrasonicLocalizer(odometer,usPoller, localizationScan, leftMotor, rightMotor,WHEEL_RADIUS, WHEEL_RADIUS, TRACK );
    final LightLocalizer lightLocalizer = new LightLocalizer(odometer,navigator,usPoller, csPoller, leftMotor, rightMotor,WHEEL_RADIUS, WHEEL_RADIUS, TRACK);
    
	do { 
      t.clear();	                           // Clear the display.
      t.drawString("        |        ", 0, 0);  // Prompt user for input
      t.drawString(" Falling| Rising ", 0, 1);
      t.drawString("  Edge  |  Edge  ", 0, 2);
      t.drawString("  <<<   |   >>>  ", 0, 3);
      t.drawString("        |        ", 0, 4);
      
      buttonChoice = Button.waitForAnyPress(); //Wait for user input
	} while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT );
      if(buttonChoice == Button.ID_LEFT){
    	odometer.start();                  
	    odometryDisplay.start();
	    usLocalizer.setMode(1);    //Set mode to falling edge
	    usLocalizer.start();
      }
      if(buttonChoice == Button.ID_RIGHT){
        odometer.start();                  
        odometryDisplay.start();
        usLocalizer.setMode(2);    //Set mode to rising edge
        usLocalizer.start();
        
      }
      
      while(usLocalizer.isComplete() == false){
        //wait
      }
      
      
      do{
      t.clear();                               // Clear the display.
      t.drawString("                 ", 0, 0);  // Prompt user for input
      t.drawString("                 ", 0, 1);
      t.drawString("                 ", 0, 2);
      t.drawString("   Press Enter   ", 0, 3);
      t.drawString("   To Continue   ", 0, 4);
      buttonChoice = Button.waitForAnyPress(); //Wait for user input
      }while(buttonChoice != Button.ID_ENTER);
      
      
      //Start Light Localizer once enter has been pressed
      t.clear();
      lightLocalizer.start();
      
      
      
      
      
      
        while (Button.waitForAnyPress() != Button.ID_ESCAPE);
        //The Program waits here, and exits if the user presses the escape button.
        System.exit(0);  
  }   
}