package fr.eurecom.sentime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import en.weimar.webis.SentimentanalysisSemEval;
import en.weimar.webis.Tweet;

public class SentimeSystem {
	
	static String PATH = "";
	private static long startTime = System.currentTimeMillis();
	
	private static void help(){
		System.err.println("Usage: <System Mode> <tweet corpus> [parameters] ... \n");
		System.err.println("SYSTEM MODE: ");
		System.err.println("train:  This mode is used for training the models.");
		System.err.println("test:   This mode is used for testing the models.");
		System.err.println("single: This mode is used for classifying a single tweet.\n");
		System.err.println("PARAMETERS: ");
		System.err.println("-arrfname         Specify the name of arffs which are used to store the model");
		System.err.println("-testmodel        Only test one sub-classifier, use 0-4 to specify which sub-classifier to be tested");
		System.err.println("-trainmodel       Only train one sub-classifier, use 0-3 to specify which sub-classifer to be trained");
		System.err.println("-bsize            Enable bagging training process and specify the size of bootstrap samples.");
		System.err.println("-experiment       Using <nostanford> to disable Stanford Sentiment System; using <noteamx> to exclude TeamX; using <nost> to exclude both systems");
		System.err.println("-disablefilter    Disable the default filter mechanism: using <train> to disable duplicate input tweets; using <test> to disable duplicate tweet filtering when scoring.");
	}
	
	public static void main(String[] args) throws Exception{
		
		// Names used for identifying arff files. If it's not specified, default arff files will be used.
		String nameOfNRCarff = "";
		String nameOfGUMLTarff = "";
		String nameOfKLUEarff = "";
		String nameOfTeamXarff = "";
		String nameOfTheOne = "";
		String nameOfOutput = "";
		
		// Specifying the size of bootstrap samples.
		int bootstrapNumber = 0;
		
		// Storing the single tweet.
		Tweet singleTweet;
		
		// System mode indicators.
		boolean bagging = false;        // When true, enable bagging training process
		boolean sub_classifier = false; // When true, train or test on one sub-classifier
		int evalmodelmode = 0;          // Indicating which model to be tested; ex: 0 represents NRC
		int trainmodelmode = 0;         // Indicating which model to be trained
		boolean stanford = true;        // When true, enable stanford sentiment system
		boolean without = false;        // When true, disable TeamX
		boolean nofilter = false;       // When true, do not filter out duplicate tweet
		boolean score = false;          // When true, use the same scoring mechanism as SemEval2015
		
		// Parse the arguments
		if(args.length<1){
			System.out.println("Usage: <System Mode> <tweet corpus> [parameters] ...\nUsing -help to see detail information.");
			return;
		}

		Options options = new Options();
	    options.addOption("arffname", true, "Name of all the arff files");
		options.addOption("testmodel", true, "Choose the model to be tested");
		options.addOption("trainmodel", true, "Choose the model to be trained");
		options.addOption("experiment", true, "Enable different experiment modes");
		options.addOption("bsize", true, "Using Bagging algorithm and set the size of bootstrap samples");
		options.addOption("disablefilter", true, "Disable filtering mechanism");
		options.addOption("help", false, "Offering tutorials");
		
		CommandLineParser parser = new GnuParser();
		try {
			CommandLine line = parser.parse(options, args);
			if(line.hasOption("arffname")){
				nameOfNRCarff = "Trained-Features-NRC_" + line.getOptionValue("arffname");
				nameOfGUMLTarff = "Trained-Features-GUMLTLT_" + line.getOptionValue("arffname");
				nameOfKLUEarff = "Trained-Features-KLUE_" + line.getOptionValue("arffname");
				nameOfTeamXarff = "Trained-Features-TeamX_" + line.getOptionValue("arffname");
				nameOfOutput = line.getOptionValue("arffname");
			}
			if(line.hasOption("testmodel")){
				evalmodelmode = Integer.parseInt(line.getOptionValue("testmodel"));
				sub_classifier = true;
			}
			if(line.hasOption("trainmodel")){
				trainmodelmode = Integer.parseInt(line.getOptionValue("trainmodel"));
				sub_classifier = true;
			}
			if(line.hasOption("experiment")){
				switch(line.getOptionValue("experiment")){
				case "nostanford":
					stanford = false;
					break;
				case "noteamx":
					without = true;
					break;
				case "nost":
					stanford = false;
					without = true;
					break;
				default:
					System.err.println("Wrong parameters for -experiment!!\nUsing -help to see detail information.");
					return;
				}
			}
			if(line.hasOption("bsize")){
				bootstrapNumber = Integer.parseInt(line.getOptionValue("bsize"));
				bagging = true;
			}
			if(line.hasOption("disablefilter")){
				switch(line.getOptionValue("disablefilter")){
				case "train":
					nofilter = true;
					break;
				case "test":
					score = true;
					break;
				default:
					System.err.println("Wrong parameters for -disablefilter!!\nUsing -help to see detail information.");
					return;
				}
			}
			if(line.hasOption("help")){
				help();
				return;
			}
	
			// Initialize the SentiME system and pass tweet corpus and parameters to it.
			String[] argList = line.getArgs();
			PATH = argList[1];
			SentimeRequestHandler sentimentanalysis = new SentimeRequestHandler(PATH, nofilter, score);
			
			switch (argList[0]){
			case "train":
				if(sub_classifier){
					switch(trainmodelmode){
					case 0: nameOfTheOne = nameOfNRCarff;
					break;
					case 1: nameOfTheOne = nameOfGUMLTarff;
					break;
					case 2: nameOfTheOne = nameOfKLUEarff;
					break;
					case 3: nameOfTheOne = nameOfTeamXarff;
					break;
					}
					sentimentanalysis.trainSystem(trainmodelmode, nameOfTheOne);
				} else if (bagging){
					sentimentanalysis.bootstrapAllSystems(trainmodelmode, nameOfOutput, bootstrapNumber, without);
				} else {
					sentimentanalysis.trainAllSystems(trainmodelmode, nameOfOutput);
				}
				break;
			case "test":
				if(sub_classifier){
					switch(evalmodelmode){
					case 0: nameOfTheOne = nameOfNRCarff;
					break;
					case 1: nameOfTheOne = nameOfGUMLTarff;
					break;
					case 2: nameOfTheOne = nameOfKLUEarff;
					break;
					case 3: nameOfTheOne = nameOfTeamXarff;
					break;
					}
					sentimentanalysis.testSystem(evalmodelmode, nameOfTheOne);
				} else {
					sentimentanalysis.testAllSystem(nameOfNRCarff, nameOfGUMLTarff, nameOfKLUEarff, nameOfTeamXarff, stanford, without);
				}
				break;
			case "single":
				BufferedReader strin=new BufferedReader(new InputStreamReader(System.in));
				System.out.println("Please enter the tweet:"); 
				String str = strin.readLine();
				singleTweet= new Tweet(str, "neutral", "1");
				SentimeRequestHandler sentimentpipline = new SentimeRequestHandler(singleTweet);
				sentimentpipline.process(nameOfNRCarff, nameOfGUMLTarff, nameOfKLUEarff);
				break;
			default:
				throw new IllegalArgumentException("Invalid mode: " + argList[0]);
			}					
		}
		catch(ParseException exp){
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}
		long endTime = System.currentTimeMillis();
        System.out.println("It took " + ((endTime - startTime) / 1000) + " seconds");	
	}
	
}
