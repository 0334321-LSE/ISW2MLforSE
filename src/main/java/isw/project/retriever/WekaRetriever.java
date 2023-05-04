package isw.project.retriever;

import isw.project.model.ClassifierEvaluation;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;


import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.supervised.instance.SMOTE;

import java.util.ArrayList;
import java.util.List;


public class WekaRetriever {

    private static final String RANDOM_FOREST = "Random Forest";
    private static final String NAIVE_BAYES = "Naive Bayes";
    private static final String IBK = "IBk";

    //Sampling type
    private static final String UNDER ="Under";
    private static final String OVER ="Over";

    private static final String SMOTE ="Smote";

    //Used classifiers
    private final NaiveBayes naiveBayesClassifier;
    private final RandomForest randomForestClassifier;
    private final IBk ibkClassifier;

    //List of various classifier evaluation
    private final List<ClassifierEvaluation> simpleNaiveBayesList;
    private final List<ClassifierEvaluation> simpleRandomForestList;
    private final List<ClassifierEvaluation> simpleIBkList;

    private final List<ClassifierEvaluation> featureNaiveBayesList;
    private final List<ClassifierEvaluation> featureRandomForestList;
    private final List<ClassifierEvaluation> featureIBkList;

    private final List<ClassifierEvaluation> undersamplingNaiveBayesList;
    private final List<ClassifierEvaluation> undersamplingRandomForestList;
    private final List<ClassifierEvaluation> undersamplingIBkList;

    private final List<ClassifierEvaluation> oversamplingNaiveBayesList;
    private final List<ClassifierEvaluation> oversamplingRandomForestList;
    private final List<ClassifierEvaluation> oversamplingIBkList;

    private final List<ClassifierEvaluation> smoteNaiveBayesList;
    private final List<ClassifierEvaluation> smoteRandomForestList;
    private final List<ClassifierEvaluation> smoteIBkList;

    private final String projName;
    private final int numIter;

    public WekaRetriever(String projName, int numIter){
        this.projName = projName;
        this.numIter = numIter;
        naiveBayesClassifier = new NaiveBayes();
        randomForestClassifier = new RandomForest();
        ibkClassifier = new IBk();

        simpleRandomForestList = new ArrayList<>();
        simpleNaiveBayesList = new ArrayList<>();
        simpleIBkList = new ArrayList<>();

        featureNaiveBayesList = new ArrayList<>();
        featureRandomForestList = new ArrayList<>();
        featureIBkList = new ArrayList<>();

        undersamplingNaiveBayesList = new ArrayList<>();
        undersamplingRandomForestList = new ArrayList<>();
        undersamplingIBkList = new ArrayList<>();

        oversamplingNaiveBayesList = new ArrayList<>();
        oversamplingRandomForestList = new ArrayList<>();
        oversamplingIBkList = new ArrayList<>();


        smoteNaiveBayesList = new ArrayList<>();
        smoteRandomForestList = new ArrayList<>();
        smoteIBkList = new ArrayList<>();
    }
    public List<ClassifierEvaluation> walkForwardValidation() throws Exception {

        for(int i = 1; i<this.numIter; i++){
            DataSource sourceTest = new DataSource("C:/Users/39388/OneDrive/Desktop/ISW2/Projects/DataRetriever/" +
                    "retrieved_data/projectClasses/"+this.projName+"/"
                    +this.projName+"_WF_"+i+"/"+this.projName+"_TE"+(i+1)+".arff");
            DataSource sourceTra = new DataSource("C:/Users/39388/OneDrive/Desktop/ISW2/Projects/DataRetriever/" +
                    "retrieved_data/projectClasses/"+this.projName+"/"
                    +this.projName+"_WF_"+i+"/"+this.projName+"_TR"+i+".arff");

            //VALIDATION WITHOUT FEATURE SELECTION AND WITHOUT SAMPLING
            Instances training = sourceTra.getDataSet();
            Instances testing = sourceTest.getDataSet();
            
            int numAttributes = training.numAttributes();
            training.setClassIndex(numAttributes - 1);
            testing.setClassIndex(numAttributes - 1);
            
            simpleValidation(i,training,testing);
            
            //VALIDATION WITH FEATURE SELECTION (BEST FIRST) AND WITHOUT SAMPLING
            //TODO ASK TO PROFESSOR WHICH SUB SET EVALUATION MUST BE USED WITH BESTFIRST
            //Evaluates the worth of an attribute by measuring the correlation between it and the class
            CfsSubsetEval subsetEval = new CfsSubsetEval();
            BestFirst search = new BestFirst();

            AttributeSelection filter = new AttributeSelection();
            filter.setEvaluator(subsetEval);
            filter.setSearch(search);
            filter.setInputFormat(training);

            Instances filteredTraining = Filter.useFilter(training, filter);
            Instances filteredTesting = Filter.useFilter(testing, filter);

            int numAttrFiltered = filteredTraining.numAttributes();
            filteredTraining.setClassIndex(numAttrFiltered - 1);
            
            bestFirstFeatureSelection(i,filteredTraining,filteredTesting);

            //VALIDATION WITH FEATURE SELECTION (BEST FIRST) AND WITH SAMPLING (UNDERSAMPLING)
            undersamplingWithFeatureSelection(i,filteredTraining,filteredTesting);

            //VALIDATION WITH FEATURE SELECTION (BEST FIRST) AND WITH SAMPLING (OVERSAMPLING)
            oversamplingWithFeatureSelection(i,filteredTraining,filteredTesting);

            //VALIDATION WITH FEATURE SELECTION (BEST FIRST) AND WITH SAMPLING (SMOTE)
            smoteWithFeatureSelection(i,filteredTraining,filteredTesting);
        }

        List<ClassifierEvaluation> allEvaluationList = new ArrayList<>();
        mergeAllListOrdered(allEvaluationList);

        return allEvaluationList;
    }

    private void mergeAllListOrdered(List<ClassifierEvaluation> allEvaluationList){
        for( int i = 0; i<simpleNaiveBayesList.size(); i++){
            allEvaluationList.add(simpleNaiveBayesList.get(i));
            allEvaluationList.add(featureNaiveBayesList.get(i));
            allEvaluationList.add(undersamplingNaiveBayesList.get(i));
            allEvaluationList.add(oversamplingNaiveBayesList.get(i));
            allEvaluationList.add(smoteNaiveBayesList.get(i));
        }

        for( int j = 0; j<simpleRandomForestList.size(); j++){
            allEvaluationList.add(simpleRandomForestList.get(j));
            allEvaluationList.add(featureRandomForestList.get(j));
            allEvaluationList.add(undersamplingRandomForestList.get(j));
            allEvaluationList.add(oversamplingRandomForestList.get(j));
            allEvaluationList.add(smoteRandomForestList.get(j));
        }

        for( int k = 0; k<simpleRandomForestList.size(); k++){
            allEvaluationList.add(simpleIBkList.get(k));
            allEvaluationList.add(featureIBkList.get(k));
            allEvaluationList.add(undersamplingIBkList.get(k));
            allEvaluationList.add(oversamplingIBkList.get(k));
            allEvaluationList.add(smoteIBkList.get(k));
        }
    }


    /** Merge all the evaluation list into one */
    private void mergeAllList(List<ClassifierEvaluation> allEvaluationList){
        allEvaluationList.addAll(simpleNaiveBayesList);
        allEvaluationList.addAll(featureNaiveBayesList);
        allEvaluationList.addAll(undersamplingNaiveBayesList);
        allEvaluationList.addAll(oversamplingNaiveBayesList);
        allEvaluationList.addAll(smoteNaiveBayesList);

        allEvaluationList.addAll(simpleRandomForestList);
        allEvaluationList.addAll(featureRandomForestList);
        allEvaluationList.addAll(undersamplingRandomForestList);
        allEvaluationList.addAll(oversamplingRandomForestList);
        allEvaluationList.addAll(smoteRandomForestList);


        allEvaluationList.addAll(simpleIBkList);
        allEvaluationList.addAll(featureIBkList);
        allEvaluationList.addAll(undersamplingIBkList);
        allEvaluationList.addAll(oversamplingIBkList);
        allEvaluationList.addAll(smoteIBkList);
    }

    /** Does the simple evaluation without any feature selection/sampling/cost sensitive */
    private void simpleValidation(int i, Instances training, Instances testing) throws Exception {

        //Build the classifiers
        naiveBayesClassifier.buildClassifier(training);
        randomForestClassifier.buildClassifier(training);
        ibkClassifier.buildClassifier(training);

        Evaluation evaluation = new Evaluation(testing);
        //simple Naive Bayes
        ClassifierEvaluation simpleNaiveBayes = new ClassifierEvaluation(this.projName, i, NAIVE_BAYES, false, "NO", false);
        simpleNaiveBayesList.add(evaluateClassifier(evaluation,simpleNaiveBayes,naiveBayesClassifier,training,testing));

        //simple RandomForest
        ClassifierEvaluation simpleRandomForest = new ClassifierEvaluation(this.projName, i, RANDOM_FOREST, false, "NO", false);
        simpleRandomForestList.add(evaluateClassifier(evaluation,simpleRandomForest,randomForestClassifier,training,testing));

        //simple IBK
        ClassifierEvaluation simpleIBk = new ClassifierEvaluation(this.projName, i, IBK, false, "NO", false);
        simpleIBkList.add(evaluateClassifier(evaluation,simpleIBk,ibkClassifier,training,testing));

    }

    /** Does validation with Best first feature selection */
    private void bestFirstFeatureSelection(int i, Instances filteredTraining, Instances filteredTesting) throws Exception {
        naiveBayesClassifier.buildClassifier(filteredTraining);
        randomForestClassifier.buildClassifier(filteredTraining);
        ibkClassifier.buildClassifier(filteredTraining);
        
        Evaluation evaluation = new Evaluation(filteredTesting);
        //Naive Bayes with feature selection
        ClassifierEvaluation featureNaiveBayes = new ClassifierEvaluation(this.projName, i, NAIVE_BAYES, true, "NO", false);
        featureNaiveBayesList.add(evaluateClassifier(evaluation,featureNaiveBayes,naiveBayesClassifier,filteredTraining,filteredTesting));

        // RandomForest with feature selection
        ClassifierEvaluation featureRandomForest = new ClassifierEvaluation(this.projName, i, RANDOM_FOREST, true, "NO", false);
        featureRandomForestList.add(evaluateClassifier(evaluation,featureRandomForest,randomForestClassifier,filteredTraining,filteredTesting));

        //IBK with feature selection
        ClassifierEvaluation featureIBk = new ClassifierEvaluation(this.projName, i, IBK, true, "NO", false);
        featureIBkList.add(evaluateClassifier(evaluation,featureIBk,ibkClassifier,filteredTraining,filteredTesting));

    }

    /** Does validation with Best first feature selection and undersampling */
    private void undersamplingWithFeatureSelection(int i, Instances filteredTraining, Instances filteredTesting) throws Exception {


        SpreadSubsample spreadSubsample = new SpreadSubsample();
        spreadSubsample.setInputFormat(filteredTraining);
        spreadSubsample.setOptions(new String[] {"-M", "1.0"});
        FilteredClassifier fc = new FilteredClassifier();
        fc.setFilter(spreadSubsample);

        Evaluation evaluation = new Evaluation(filteredTesting);
        //Naive Bayes with feature selection and undersampling
        fc.setClassifier(naiveBayesClassifier);
        fc.buildClassifier(filteredTraining);
        ClassifierEvaluation undersamplingNaiveBayes = new ClassifierEvaluation(this.projName, i, NAIVE_BAYES, true, UNDER, false);
        undersamplingNaiveBayesList.add(evaluateClassifier(evaluation,undersamplingNaiveBayes,fc,filteredTraining,filteredTesting));

        // RandomForest with feature selection and undersampling
        fc.setClassifier(randomForestClassifier);
        fc.buildClassifier(filteredTraining);
        ClassifierEvaluation undersamplingRandomForest = new ClassifierEvaluation(this.projName, i, RANDOM_FOREST, true, UNDER, false);
        undersamplingRandomForestList.add(evaluateClassifier(evaluation,undersamplingRandomForest,fc,filteredTraining,filteredTesting));

        //IBK with feature selection and undersampling
        fc.setClassifier(ibkClassifier);
        fc.buildClassifier(filteredTraining);
        ClassifierEvaluation undersamplingIBk = new ClassifierEvaluation(this.projName, i, IBK, true, UNDER, false);
        undersamplingIBkList.add(evaluateClassifier(evaluation,undersamplingIBk,fc,filteredTraining,filteredTesting));

    }

    /** Does validation with Best first feature selection and oversampling */
    private void oversamplingWithFeatureSelection(int i, Instances filteredTraining, Instances filteredTesting) throws Exception {

        //sampleSizePercent is equal to Y where Y/2 is equal to the percentage of the majority instance
        int notBuggy = getIsntBuggyIstanceNumber(filteredTraining);
        double percentage = ((double) notBuggy /filteredTraining.size())*100;
        System.out.println(i+") "+notBuggy+ " percentage is :" + percentage);

        Resample resample = new Resample();
        resample.setInputFormat(filteredTraining);
        resample.setOptions(new String[] {"-B", "1.0","-Z", Double.toString(2*percentage)});
        FilteredClassifier fc = new FilteredClassifier();
        fc.setFilter(resample);

        Evaluation evaluation = new Evaluation(filteredTesting);
        //Naive Bayes with feature selection and oversampling
        fc.setClassifier(naiveBayesClassifier);
        fc.buildClassifier(filteredTraining);
        ClassifierEvaluation oversamplingNaiveBayes = new ClassifierEvaluation(this.projName, i, NAIVE_BAYES, true, OVER, false);
        oversamplingNaiveBayesList.add(evaluateClassifier(evaluation,oversamplingNaiveBayes,fc,filteredTraining,filteredTesting));

        // RandomForest with feature selection and oversampling
        fc.setClassifier(randomForestClassifier);
        fc.buildClassifier(filteredTraining);
        ClassifierEvaluation oversamplingRandomForest = new ClassifierEvaluation(this.projName, i, RANDOM_FOREST, true, OVER, false);
        oversamplingRandomForestList.add(evaluateClassifier(evaluation,oversamplingRandomForest,fc,filteredTraining,filteredTesting));

        //IBK with feature selection and oversampling
        fc.setClassifier(ibkClassifier);
        fc.buildClassifier(filteredTraining);
        ClassifierEvaluation oversamplingIBk = new ClassifierEvaluation(this.projName, i, IBK, true, OVER, false);
        oversamplingIBkList.add(evaluateClassifier(evaluation,oversamplingIBk,fc,filteredTraining,filteredTesting));

    }

    /** Does validation with Best first feature selection and smote */
    private void smoteWithFeatureSelection(int i, Instances filteredTraining, Instances filteredTesting) throws Exception {

        int notBuggy = getIsntBuggyIstanceNumber(filteredTraining);
        int buggy = filteredTraining.size()-notBuggy;

        double percentage = ((double) (notBuggy-buggy)/buggy) * 100;
        //
        if ( buggy == 0 ) percentage = 0;
        System.out.println(i+") "+notBuggy+ " percentage is :" + percentage);

        FilteredClassifier fc = new FilteredClassifier();

        SMOTE smote = new SMOTE();
        smote.setInputFormat(filteredTraining);
        smote.setClassValue("1");
        smote.setPercentage(percentage);

        fc.setFilter(smote);

        Evaluation evaluation = new Evaluation(filteredTesting);
        //Naive Bayes with feature selection and oversampling
        fc.setClassifier(naiveBayesClassifier);
        fc.buildClassifier(filteredTraining);
        ClassifierEvaluation smoteNaiveBayes = new ClassifierEvaluation(this.projName, i, NAIVE_BAYES, true, SMOTE, false);
        smoteNaiveBayesList.add(evaluateClassifier(evaluation,smoteNaiveBayes,fc,filteredTraining,filteredTesting));

        // RandomForest with feature selection and oversampling
        fc.setClassifier(randomForestClassifier);
        fc.buildClassifier(filteredTraining);
        ClassifierEvaluation smoteRandomForest = new ClassifierEvaluation(this.projName, i, RANDOM_FOREST, true, SMOTE, false);
        smoteRandomForestList.add(evaluateClassifier(evaluation,smoteRandomForest,fc,filteredTraining,filteredTesting));

        //IBK with feature selection and oversampling
        fc.setClassifier(ibkClassifier);
        fc.buildClassifier(filteredTraining);
        ClassifierEvaluation smoteIBk = new ClassifierEvaluation(this.projName, i, IBK, true, SMOTE, false);
        smoteIBkList.add(evaluateClassifier(evaluation,smoteIBk,fc,filteredTraining,filteredTesting));

    }



    private int getIsntBuggyIstanceNumber(Instances training){
        int notBuggyInstance = 0;
        for (Instance instance: training){
            if ( instance.toString(instance.numAttributes()-1).equals("false") ) notBuggyInstance++;
        }
        return notBuggyInstance;
    }

    private static ClassifierEvaluation evaluateClassifier(Evaluation evaluation, ClassifierEvaluation classifierEvaluation, AbstractClassifier classifierType,Instances training,Instances testing) throws Exception {
        evaluation.evaluateModel(classifierType,testing);
        classifierEvaluation.setTrainingPercent(100.0*training.numInstances()/(training.numInstances()+testing.numInstances()));
        classifierEvaluation.setPrecision(evaluation.precision(0));
        classifierEvaluation.setRecall(evaluation.recall(0));
        classifierEvaluation.setAuc(evaluation.areaUnderROC(0));
        classifierEvaluation.setKappa(evaluation.kappa());
        classifierEvaluation.setTp(evaluation.numTruePositives(0));
        classifierEvaluation.setFp(evaluation.numFalsePositives(0));
        classifierEvaluation.setTn(evaluation.numTrueNegatives(0));
        classifierEvaluation.setFn(evaluation.numFalseNegatives(0));
        return classifierEvaluation;
    }
}
