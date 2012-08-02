package com.ipeirotis.gal.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ipeirotis.gal.scripts.AssignedLabel;
import com.ipeirotis.gal.scripts.Category;
import com.ipeirotis.gal.scripts.CorrectLabel;
import com.ipeirotis.gal.scripts.DawidSkene;
import com.ipeirotis.gal.scripts.MisclassificationCost;
import com.ipeirotis.utils.Utils;

public class Engine {
	private Set<Category> categories;

	private DawidSkene ds;

	private Set<MisclassificationCost> costs;

	private Set<AssignedLabel> labels;

	private Set<CorrectLabel> correct;

	private Set<CorrectLabel> evaluation;

	private EngineContext ctx;

	public Engine(EngineContext ctx) {
		this.ctx = ctx;
	}

	public Set<Category> getCategories() {
		return categories;
	}

	public void setCategories(Set<Category> categories) {
		this.categories = categories;
	}

	public DawidSkene getDs() {
		return ds;
	}

	public void setDs(DawidSkene ds) {
		this.ds = ds;
	}

	public Set<MisclassificationCost> getCosts() {
		return costs;
	}

	public void setCosts(Set<MisclassificationCost> costs) {
		this.costs = costs;
	}

	public Set<AssignedLabel> getLabels() {
		return labels;
	}

	public void setLabels(Set<AssignedLabel> labels) {
		this.labels = labels;
	}

	public Set<CorrectLabel> getCorrect() {
		return correct;
	}

	public void setCorrect(Set<CorrectLabel> correct) {
		this.correct = correct;
	}

	public Set<CorrectLabel> getEvaluation() {
		return evaluation;
	}

	public void setEvaluation(Set<CorrectLabel> evaluation) {
		this.evaluation = evaluation;
	}

	public void execute() {
		setCategories(loadCategories(ctx.getCategoriesFile()));

		setDs(new DawidSkene(getCategories()));
		if (getDs().fixedPriors() == true)
			println("Using fixed priors.");
		else
			println("Using data-inferred priors.");

		setCosts(loadCosts(ctx.getCostFile()));

		assert (getCosts().size() == getCategories().size() * getCategories().size());

		for (MisclassificationCost mcc : getCosts()) {
			getDs().addMisclassificationCost(mcc);
		}

		setLabels(loadWorkerAssignedLabels(ctx.getInputFile()));

		int al = 0;

		for (AssignedLabel l : getLabels()) {
			if (++al % 1000 == 0)
				printRaw(".");
			getDs().addAssignedLabel(l);
		}
		print("");
		println("%d worker-assigned labels loaded.", getLabels().size());

		setCorrect(loadGoldLabels(ctx.getCorrectFile()));

		int cl = 0;
		for (CorrectLabel l : getCorrect()) {
			if (++cl % 1000 == 0)
				printRaw(".");
			getDs().addCorrectLabel(l);
		}
		print("");
		println("%d correct labels loaded.", getCorrect().size());

		setEvaluation(loadEvaluationLabels(ctx.getEvaluationFile()));
		int el = 0;
		for (CorrectLabel l : getEvaluation()) {
			if (++el % 1000 == 0)
				printRaw(".");
			getDs().addEvaluationLabel(l);
		}
		print("");
		println(getEvaluation().size() + " evaluation labels loaded.");

		// We compute the evaluation-based confusion matrix for the workers
		getDs().evaluateWorkers();

		//ds.estimate(1);
		//HashMap<String, String> prior_voting = saveMajorityVote(verbose, ds);

		println("");
		println("Running the Dawid&Skene algorithm");
		for (int i = 0; i < ctx.getNumIterations(); i++) {
			println("Iteration: %d", i);
			// ds.estimate(iterations);
			getDs().estimate(1);
		}
		println("Done\n");

		if (ctx.isMinimalMode()) {
			// minimal mode: don't do any fancy stuff, just print object ids and their labels computed by D&S to STDOUT.
			// construct and print the result on-the-fly to save memory
			int resultLength;
			char delimiter = '\t';

			if (ctx.isVerbose()) {
				print("RESULTS");
			}

			for (Map.Entry<String, String> entry : getDs().getMajorityVote().entrySet()) {
				resultLength = entry.getKey().length() + entry.getValue().length() + 1;
				StringBuilder result = new StringBuilder(resultLength);

				result.append(entry.getKey());
				result.append(delimiter);
				result.append(entry.getValue());

				System.out.println(result.toString());
			}
		} else {
			saveWorkerQuality(getDs());

			saveObjectResults(getDs());

			saveCategoryPriors(getDs());

			//HashMap<String, String> posterior_voting = saveDawidSkeneVote(verbose, ds);

			//saveDifferences(verbose, ds, prior_voting, posterior_voting);
		}
	}


	/*
	private static void saveDifferences(boolean verbose, DawidSkene ds, HashMap<String, String> prior_voting,
			HashMap<String, String> posterior_voting) {

		println("");
		System.out
				.println("Computing the differences between naive majority vote and Dawid&Skene (see also file results/differences-with-majority-vote.txt)");
		String differences = ds.printDiffVote(prior_voting, posterior_voting);
		if (verbose) {
			println("=======DIFFERENCES WITH MAJORITY VOTE========");
			println(differences);
			println("=============================================");
		}
		Utils.writeFile(differences, "results/differences-with-majority-vote.txt");
	}
	*/

	/**
	 * @param verbose
	 * @param ds
	 * @return
	 */
	/*
	private static HashMap<String, String> saveDawidSkeneVote(boolean verbose, DawidSkene ds) {

		// Save the vote after the D&S estimation
		println("");
		println("Estimating the Dawid & Skene object labels (see also file results/dawid-skene-results.txt)");
		HashMap<String, String> posterior_voting = ds.getMajorityVote();
		String dawidskene = ds.printVote();
		if (verbose) {
			println("=======DAWID&SKENE RESULTS========");
			println(dawidskene);
			println("==================================");
		}
		Utils.writeFile(dawidskene, "results/dawid-skene-results.txt");
		return posterior_voting;
	}
	*/

	/**
	 * @param ds
	 */
	private void saveCategoryPriors(DawidSkene ds) {

		// Save the probability that an object belongs to each class
		println("Saving prior probabilities to 'results/priors.txt'");

		String priors = ds.printPriors();

		if (ctx.isPrintResults()) {
			println("Printing prior probabilities (see also file results/priors.txt):");
			println("======= PRIOR PROBABILITIES ========");
			println(priors);
			println("====================================");
		}
		Utils.writeFile(priors, "results/priors.txt");
	}

	/**
	 * @param ds
	 */
	private void saveObjectResults(DawidSkene ds) {

		// Save the probability that an object belongs to each class
		println("Saving category probabilities for objects to 'results/object-probabilities.txt'");

		String objectProbs = ds.printObjectClassProbabilities();

		if (ctx.isPrintResults()) {
			println("Printing category probabilities for objects (see also file results/object-probabilities.txt):");
			println("======= CATEGORY PROBABILITIES ========");
			println(objectProbs);
			println("=======================================");
		}
		Utils.writeFile(objectProbs, "results/object-probabilities.txt");
	}

	/**
	 * @param ds
	 */
	private void saveWorkerQuality(DawidSkene ds) {

		// Save the estimated quality characteristics for each worker
		print("Estimating worker quality");

		if (ctx.isVerbose()) {
			System.out.println(" (see file 'results/worker-statistics-summary.txt' and" +
					" 'results/worker-statistics-detailed.txt')");
			System.out.println();
		}

		String summary_report = ds.printAllWorkerScores(false);
		String detailed_report = ds.printAllWorkerScores(true);

		if (ctx.isPrintResults()) {
			println("Printing worker quality statistics (see also file 'results/worker-statistics-summary.txt'):");
			println("======= WORKER QUALITY STATISTICS =======");
			println(summary_report);
			println("=========================================");
		}
		Utils.writeFile(summary_report, "results/worker-statistics-summary.txt");
		Utils.writeFile(detailed_report, "results/worker-statistics-detailed.txt");
	}

	/**
	 * @param verbose
	 * @param ds
	 * @return
	 */
	/*
	private static HashMap<String, String> saveMajorityVote(boolean verbose, DawidSkene ds) {

		// Save the majority vote before the D&S estimation
		println("");
		println("Estimating the naive majority vote (see also file results/naive-majority-vote.txt)");
		HashMap<String, String> prior_voting = ds.getMajorityVote();
		String majority = ds.printVote();
		if (verbose) {
			println("=======NAIVE MAJORITY VOTE========");
			println(majority);
			println("==================================");
		}
		Utils.writeFile(majority, "results/naive-majority-vote.txt");
		return prior_voting;
	}*/

	/**
	 * @param correctfile
	 * @return
	 */
	private Set<CorrectLabel> loadGoldLabels(String correctfile) {

		// We load the "gold" cases (if any)
		println("");
		println("Loading file with correct labels. ");
		BufferedReader reader = Utils.getFileReader(correctfile);
		Set<CorrectLabel> correct = getCorrectLabels(reader);
		return correct;
	}

	/**
	 * @param evalfile
	 * @return
	 */
	private Set<CorrectLabel> loadEvaluationLabels(String evalfile) {

		// We load the "gold" cases (if any)
		println("");
		println("Loading file with evaluation labels. ");
		String[] lines_correct = Utils.getFileContent(evalfile).split("\n");
		println("File contained %d entries.", lines_correct.length);
		Set<CorrectLabel> correct = getEvaluationLabels(lines_correct);
		return correct;
	}

	public Set<AssignedLabel> getAssignedLabels(BufferedReader reader) {

		Set<AssignedLabel> labels = new HashSet<AssignedLabel>();
		int cnt = 1;
		String line;

		while (true) {
			try {
				line = reader.readLine();
				if (line == null) {
					break;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				break;
			}
			String[] entries = line.split("\t");
			if (entries.length != 3) {
				throw new IllegalArgumentException("Error while loading from assigned labels file (line #" + cnt + "): " + line);
			}
			cnt++;

			String workername = entries[0];
			String objectname = entries[1];
			String categoryname = entries[2];

			AssignedLabel al = new AssignedLabel(workername, objectname, categoryname);
			labels.add(al);
		}
		return labels;
	}

	public Set<Category> getCategories(String[] lines) {

		Set<Category> categories = new HashSet<Category>();
		for (String line : lines) {
			// First we check if we have fixed priors or not
			// If we have fixed priors, we have a TAB character
			// after the name of each category, followed by the prior value
			String[] l = line.split("\t");
			if (l.length == 1) {
				Category c = new Category(line);
				categories.add(c);
			} else if (l.length == 2) {
				String name = l[0];
				Double prior = new Double(l[1]);
				Category c = new Category(name);
				c.setPrior(prior);
				categories.add(c);
			}
		}
		return categories;
	}

	public Set<MisclassificationCost> getClassificationCost(String[] lines) {

		Set<MisclassificationCost> labels = new HashSet<MisclassificationCost>();
		int cnt = 1;
		for (String line : lines) {
			String[] entries = line.split("\t");
			if (entries.length != 3) {
				throw new IllegalArgumentException("Error while loading from assigned labels file (line " + cnt + "):" + line);
			}
			cnt++;

			String from = entries[0];
			String to = entries[1];
			Double cost = Double.parseDouble(entries[2]);

			MisclassificationCost mcc = new MisclassificationCost(from, to, cost);
			labels.add(mcc);
		}
		return labels;
	}

	public Set<CorrectLabel> getCorrectLabels(BufferedReader reader) {

		Set<CorrectLabel> labels = new HashSet<CorrectLabel>();
		int cnt = 1;
		String line;


		while (true) {
			try {
				line = reader.readLine();
				if (line == null) {
					break;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				break;
			}

			if (line.isEmpty()) {
				continue;
			}

			String[] entries = line.split("\t");

			if (entries.length != 2) {
				throw new IllegalArgumentException("Error while loading from correct labels file (line " + cnt + "):" + line);
			}
			cnt++;

			String objectname = entries[0];
			String categoryname = entries[1];

			CorrectLabel cl = new CorrectLabel(objectname, categoryname);
			labels.add(cl);
		}
		return labels;
	}

	public Set<CorrectLabel> getEvaluationLabels(String[] lines) {

		Set<CorrectLabel> labels = new HashSet<CorrectLabel>();
		for (String line : lines) {
			String[] entries = line.split("\t");
			if (entries.length != 2) {
				// evaluation file is optional
				continue;
			}

			String objectname = entries[0];
			String categoryname = entries[1];

			CorrectLabel cl = new CorrectLabel(objectname, categoryname);
			labels.add(cl);
		}
		return labels;
	}

	/**
	 * @param inputfile
	 * @return
	 */
	private Set<AssignedLabel> loadWorkerAssignedLabels(String inputfile) {

		// We load the labels assigned by the workers on the different objects
		println("");
		println("Loading file with assigned labels. ");
		BufferedReader reader = Utils.getFileReader(inputfile);
		Set<AssignedLabel> labels = getAssignedLabels(reader);
		return labels;
	}

	/**
	 * @param costfile
	 * @return
	 */
	private Set<MisclassificationCost> loadCosts(String costfile) {

		// We load the cost file. The file should have exactly n^2 lines
		// where n is the number of categories.
		// TODO: Later, we can also allow an empty file, and assume a default 0/1 loss function.
		println("");
		println("Loading cost file.");
		String[] lines_cost = Utils.getFileContent(costfile).split("\n");
		// assert (lines_cost.length == categories.size() * categories.size());
		println("File contains " + lines_cost.length + " entries.");
		Set<MisclassificationCost> costs = getClassificationCost(lines_cost);
		return costs;
	}

	/**
	 * @param categoriesfile
	 * @return
	 */
	private Set<Category> loadCategories(String categoriesfile) {
		println("");
		println("Loading categories file.");
		String[] lines_categories = Utils.getFileContent(categoriesfile).split("\n");
		println("File contains " + lines_categories.length + " categories.");
		Set<Category> categories = getCategories(lines_categories);
		return categories;
	}

	public void println(String mask, Object... args) {
		print(mask + "\n", args);
	}

	public void print(String mask, Object... args) {
		if (!ctx.isVerbose())
			return;

		String message;

		if (args.length > 0) {
			message = String.format(mask, args);
		} else {
			// without format arguments, print the mask/string as-is
			message = mask;
		}

		System.out.println(message);
	}

	public void printRaw(String mask, Object... args) {
		if (!ctx.isVerbose())
			return;

		String message;

		if (args.length > 0) {
			message = String.format(mask, args);
		} else {
			// without format arguments, print the mask/string as-is
			message = mask;
		}

		System.out.print(message);
	}
}