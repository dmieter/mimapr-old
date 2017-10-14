package project.engine.former.deadline;

import project.engine.data.ResourceLine;
import project.engine.data.ResourceRequest;
import project.engine.data.VOEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Shankara on 06.04.2014.
 */
class ValueFinder {

	public static double findSpecificValue(ResourceRequest request, VOEnvironment environment, int periodStart,
										   int cycleLength, DeadlineFormerSettings.ValueFinderSettings settings) {
		List<ResourceLine> fittingLines = new ArrayList<ResourceLine>();
		for (ResourceLine line : environment.resourceLines) {
			if (line.getSpeed() >= request.resourceSpeed)
				fittingLines.add(line);
		}
		if (fittingLines.isEmpty())
			return Double.NEGATIVE_INFINITY;

		// Q
		double q0 = 0;
		for (int i = 0; i < fittingLines.size(); i++) {
			q0 = (q0 * i + fittingLines.get(i).price / fittingLines.get(i).getSpeed()) / (i + 1);
		}
		double Q = (settings.KQ / settings.CQ) * (request.priceMax / request.resourceSpeed / (q0) - settings.CQ);

		// n
		double n = (settings.KN / settings.CN) * (settings.CN - (double) request.resourceNeed / fittingLines.size());

		// l
		double l = (settings.KL / settings.CL) * (settings.CL - request.time * request.resourceSpeed /
				DeadlineFormerVoeUtils.getAverageSlotSpecificLength(fittingLines, periodStart, cycleLength));

		// V
		double vs =0;
		for (ResourceLine line : fittingLines) {
			vs += DeadlineFormerVoeUtils.getSumSlotLength(line, periodStart, cycleLength) * line.getSpeed();
		}
		double V = (settings.KV / settings.CV) *
				(settings.CV - request.time * request.resourceSpeed * request.resourceNeed / vs);

		return Q + n + l + V;
	}
}
