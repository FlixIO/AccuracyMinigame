package io.flixion.accuracy.region;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegionMappedLocation {
	private Map<UUID, PointData> blockEntityMapping = new HashMap<>();

	public Map<UUID, PointData> getBlockEntityMapping() {
		return blockEntityMapping;
	}
}
