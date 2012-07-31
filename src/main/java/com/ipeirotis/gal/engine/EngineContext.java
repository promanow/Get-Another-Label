package com.ipeirotis.gal.engine;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class EngineContext {
	@Argument(index=0, metaVar="<categoriesfile>", required=true, usage="The <categoriesfile> can also be used to define the prior values for the different categories, instead of letting the priors be defined by the data. In that case, it becomes a tab-separated file and each line has the form <category><tab><prior>")
	String categoriesFile = "";

	public String getCategoriesFile() {
		return categoriesFile;
	}

	public void setCategoriesFile(String categoriesfile) {
		this.categoriesFile = categoriesfile;
	}

	@Argument(index=1, metaVar="<inputfile>", required=true, usage="A tab-separated text file. Each line has the form <workerid><tab><objectid><tab><assigned_label> and records the label that the given worker gave to that object")
	String inputFile = "";

	public String getInputFile() {
		return inputFile;
	}

	public void setInputFile(String inputfile) {
		this.inputFile = inputfile;
	}

	@Argument(index=2, metaVar="<correctfile>", required=true, usage="A tab-separated text file. Each line has the form <objectid><tab><assigned_label> and records the correct labels for whatever objects we have them.")
	String correctFile = "";

	public String getCorrectFile() {
		return correctFile;
	}

	public void setCorrectFile(String correctfile) {
		this.correctFile = correctfile;
	}

	@Argument(index=3, metaVar="<costfile>", usage="A tab-separated text file. Each line has the form <from_class><tab><to_class><tab><classification_cost> and records the classification cost of classifying an object thatbelongs to the `from_class` into the `to_class`.", required=true)
	String costFile = "";

	public String getCostFile() {
		return costFile;
	}

	public void setCostFile(String costfile) {
		this.costFile = costfile;
	}

	@Argument(index=4, metaVar="<evaluationfile>", usage="Evaluation File (TBD)", required=true)
	String evaluationFile = "";

	public String getEvaluationFile() {
		return evaluationFile;
	}

	public void setEvaluationFile(String evaluationfile) {
		this.evaluationFile = evaluationfile;
	}

	@Option(name="--iterations", usage="is the number of times to run the algorithm. Even a value of 10 (the default) less often works well.", metaVar="<num-iterations>")
	int numIterations = 10;

	public int getNumIterations() {
		return numIterations;
	}

	public void setNumIterations(int iterations) {
		this.numIterations = iterations;
	}

	@Option(name="--verbose", usage="Verbose Mode? (off by default)")
	boolean verbose = false;

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

    @Option(name="--print", usage="Print results to STDOUT? (off by default)")
    boolean printResults = false;

    public boolean isPrintResults() {
        return printResults;
    }

    public void setPrintResults(boolean printResults) {
        this.printResults = printResults;
    }

    @Option(name="--minimal", usage="Minimal mode? -- Only prints object ids and their computed labels to STDOUT in" +
            " format: '<objectid><tab><label>'. Does not save any files. IGNORES --verbose and --print. Useful for" +
            " using the program with external tools.")
    boolean minimalMode = false;

    public boolean isMinimalMode() {
        return minimalMode;
    }

    public void setMinimalMode(boolean minimalMode) {
        this.minimalMode = minimalMode;
    }
}
