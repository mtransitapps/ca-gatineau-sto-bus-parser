package org.mtransit.parser.ca_gatineau_sto_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// http://www.sto.ca/index.php?id=575
// http://www.sto.ca/index.php?id=596
// http://www.contenu.sto.ca/GTFS/GTFS.zip
public class GatineauSTOBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-gatineau-sto-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new GatineauSTOBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating STO bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		System.out.printf("\nGenerating STO bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(gRoute.getRouteShortName()); // using route short name as route ID
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if ("33".equals(gRoute.getRouteShortName())) {
			return "Station De La Cité / Cegep Gabrielle-Roy / Ottawa";
		}
		return cleanRouteLongName(gRoute);
	}

	private String cleanRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		routeLongName = CleanUtils.cleanSlashes(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_BLUE = "007F89"; // BLUE PANTONE 7474 (from Corporate Logo Usage PDF)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String REGULAR_COLOR = "231F20"; // "000000"; // BLACK (from PDF)
	private static final String PEAK_COLOR = "9B0078"; // VIOLET (from PDF)
	private static final String RB100_COLOR = "0067AC"; // BLUE (from PDF)
	private static final String RB200_COLOR = "DA002E"; // RED (from PDF)

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 11: return PEAK_COLOR;
			case 15: return PEAK_COLOR;
			case 17: return PEAK_COLOR;
			case 20: return PEAK_COLOR;
			case 21: return REGULAR_COLOR;
			case 22: return PEAK_COLOR;
			case 24: return PEAK_COLOR;
			case 25: return PEAK_COLOR;
			case 26: return PEAK_COLOR;
			case 27: return PEAK_COLOR;
			case 28: return PEAK_COLOR;
			case 29: return PEAK_COLOR;
			case 31: return REGULAR_COLOR; // OCCASIONAL_COLOR;
			case 32: return PEAK_COLOR;
			case 33: return REGULAR_COLOR; // OCCASIONAL_COLOR;
			case 35: return REGULAR_COLOR; // OCCASIONAL_COLOR;
			case 36: return REGULAR_COLOR; // OCCASIONAL_COLOR;
			case 37: return REGULAR_COLOR; // OCCASIONAL_COLOR;
			case 38: return REGULAR_COLOR; // OCCASIONAL_COLOR;
			case 39: return REGULAR_COLOR;
			case 40: return PEAK_COLOR;
			case 41: return PEAK_COLOR;
			case 44: return PEAK_COLOR;
			case 45: return PEAK_COLOR;
			case 46: return PEAK_COLOR;
			case 47: return PEAK_COLOR;
			case 48: return PEAK_COLOR;
			case 49: return REGULAR_COLOR;
			case 50: return PEAK_COLOR;
			case 51: return REGULAR_COLOR;
			case 52: return REGULAR_COLOR;
			case 53: return REGULAR_COLOR;
			case 54: return PEAK_COLOR;
			case 57: return REGULAR_COLOR;
			case 58: return PEAK_COLOR;
			case 59: return REGULAR_COLOR; // OCCASIONAL_COLOR;
			case 60: return PEAK_COLOR;
			case 61: return PEAK_COLOR;
			case 62: return REGULAR_COLOR;
			case 63: return REGULAR_COLOR;
			case 64: return REGULAR_COLOR;
			case 65: return REGULAR_COLOR;
			case 66: return REGULAR_COLOR;
			case 67: return PEAK_COLOR;
			case 68: return REGULAR_COLOR; // RAPIDBUS_COLOR
			case 69: return REGULAR_COLOR;
			case 71: return REGULAR_COLOR;
			case 73: return REGULAR_COLOR;
			case 74: return PEAK_COLOR;
			case 75: return REGULAR_COLOR;
			case 76: return REGULAR_COLOR;
			case 77: return REGULAR_COLOR;
			case 78: return REGULAR_COLOR;
			case 79: return REGULAR_COLOR;
			case 85: return PEAK_COLOR;
			case 88: return PEAK_COLOR;
			case 93: return PEAK_COLOR; // RAPIDBUS_COLOR
			case 95: return PEAK_COLOR; // RAPIDBUS_COLOR
			case 94: return PEAK_COLOR;
			case 97: return REGULAR_COLOR;
			case 98: return PEAK_COLOR;
			case 100: return RB100_COLOR; // RAPIDBUS_COLOR
			case 200: return RB200_COLOR; // RAPIDBUS_COLOR
			case 300: return REGULAR_COLOR; // RAPIDBUS_COLOR
			case 400: return REGULAR_COLOR; // RAPIDBUS_COLOR
			case 800: return PEAK_COLOR; // RAPIDBUS_COLOR
			case 810: return PEAK_COLOR; // RAPIDBUS_COLOR
			// @formatter:on
			}
			System.out.printf("\nUnexpected route color %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return super.getRouteColor(gRoute);
	}

	private static final String SLASH = " / ";
	private static final String STATION_ = ""; // "Ston ";
	private static final String LABROSSE_STATION = STATION_ + "Labrosse";
	private static final String MUSEE_CANADIEN_HISTOIRE_SHORT = "Musée de l'Histoire";
	private static final String FREEMAN = "Freeman";
	private static final String OTTAWA = "Ottawa";
	private static final String PLACE_D_ACCUEIL = "Place d'Accueil";
	private static final String DE_LA_CITÉ = "Cité"; // De La
	private static final String LORRAIN = "Lorrain";
	private static final String RIVERMEAD = "Rivermead";
	private static final String P_O_B_ALLUM = "P-O-B Allum";
	private static final String CEGEP_ = "Cegep ";
	private static final String CEGEP_GABRIELLE_ROY = CEGEP_ + "Gabrielle-Roy";
	private static final String DE_LA_GALÈNE = "Galène"; // De La
	private static final String DES_TREMBLES = "Trembles"; // Des
	private static final String PLATEAU = "Plateau";
	private static final String OTTAWA_MUSEE_HISTOIRE = OTTAWA + SLASH + MUSEE_CANADIEN_HISTOIRE_SHORT;

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(26l, new RouteTripSpec(26l, //
				0, MTrip.HEADSIGN_TYPE_STRING, PLATEAU, //
				1, MTrip.HEADSIGN_TYPE_STRING, OTTAWA) //
				.addTripSort(0, //
						Arrays.asList(new String[] { "5026", "2510", "2769" })) //
				.addTripSort(1, //
						Arrays.asList(new String[] { "2767", "2508", "5016" })) //
				.compileBothTripSort());
		map2.put(33l, new RouteTripSpec(33l, //
				0, MTrip.HEADSIGN_TYPE_STRING, DE_LA_CITÉ, //
				1, MTrip.HEADSIGN_TYPE_STRING, MUSEE_CANADIEN_HISTOIRE_SHORT) //
				.addTripSort(0, //
						Arrays.asList(new String[] { "2618", "2692", "2010", "2155", "3500" })) //
				.addTripSort(1, //
						Arrays.asList(new String[] { "3501", "2155", "2153", "2011", "2618A" })) //
				.compileBothTripSort());
		map2.put(79l, new RouteTripSpec(79l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LORRAIN, // St-Thomas
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LABROSSE_STATION) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3991", "4476", "4502" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4502", "4167", "8103" })) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		String tripHeadsign = gTrip.getTripHeadsign();
		if (StringUtils.isEmpty(tripHeadsign)) {
			tripHeadsign = mRoute.getLongName();
		}
		if (mRoute.getId() == 21l) {
			if (OTTAWA_MUSEE_HISTOIRE.equalsIgnoreCase(mTrip.getHeadsignValue())) {
				mTrip.setHeadsignString(MUSEE_CANADIEN_HISTOIRE_SHORT, gTrip.getDirectionId());
				return;
			}
		}
		mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		if (mTrip.getRouteId() == 11l) {
			if (mTrip.getHeadsignId() == 1) {
				mTrip.setHeadsignString(PLACE_D_ACCUEIL, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 17l) {
			if (mTrip.getHeadsignId() == 1) {
				mTrip.setHeadsignString(PLACE_D_ACCUEIL, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 20l) {
			if (mTrip.getHeadsignId() == 1) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 21l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(FREEMAN, mTrip.getHeadsignId());
				return true;
			} else if (mTrip.getHeadsignId() == 1) {
				mTrip.setHeadsignString(MUSEE_CANADIEN_HISTOIRE_SHORT, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 24l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(PLATEAU, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 25l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(PLATEAU, mTrip.getHeadsignId());
				return true;
			} else if (mTrip.getHeadsignId() == 1) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 29l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(DES_TREMBLES, mTrip.getHeadsignId());
				return true;
			} else if (mTrip.getHeadsignId() == 1) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 31l) {
			if (mTrip.getHeadsignId() == 1) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 35l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(DE_LA_GALÈNE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 36l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 37l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 38l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(FREEMAN, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 39l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(FREEMAN, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 41l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(P_O_B_ALLUM, mTrip.getHeadsignId());
				return true;
			} else if (mTrip.getHeadsignId() == 1) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 49l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY, mTrip.getHeadsignId());
				return true;
			} else if (mTrip.getHeadsignId() == 1) {
				mTrip.setHeadsignString(RIVERMEAD, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 50l) {
			if (mTrip.getHeadsignId() == 1) {
				mTrip.setHeadsignString(P_O_B_ALLUM, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 51l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(P_O_B_ALLUM, mTrip.getHeadsignId());
				return true;
			} else if (mTrip.getHeadsignId() == 1) {
				mTrip.setHeadsignString(RIVERMEAD, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 52l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(P_O_B_ALLUM, mTrip.getHeadsignId());
				return true;
			} else if (mTrip.getHeadsignId() == 1) {
				mTrip.setHeadsignString(RIVERMEAD, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 53l) {
			if (mTrip.getHeadsignId() == 0) {
				mTrip.setHeadsignString(P_O_B_ALLUM, mTrip.getHeadsignId());
				return true;
			} else if (mTrip.getHeadsignId() == 1) {
				mTrip.setHeadsignString(RIVERMEAD, mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnexpected trips to merge: %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern TO = Pattern.compile("((^|\\W){1}(to)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final Pattern VIA = Pattern.compile("((^|\\W){1}(via)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	private static final Pattern MUSEE_CANADIEN_HISTOIRE = Pattern
			.compile("((^|\\W){1}(mus[e|é]e canadien de l'histoire)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String MUSEE_CANADIEN_HISTOIRE_REPLACEMENT = "$2" + MUSEE_CANADIEN_HISTOIRE_SHORT + "$4";

	private static final Pattern CLEAN_STATION = Pattern.compile("((^|\\W){1}(station|ston|sta)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_STATION_REPLACEMENT = "$2" + STATION_ + "$4";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		Matcher matcherTO = TO.matcher(tripHeadsign);
		if (matcherTO.find()) {
			String gTripHeadsignAfterTO = tripHeadsign.substring(matcherTO.end());
			tripHeadsign = gTripHeadsignAfterTO;
		}
		Matcher matcherVIA = VIA.matcher(tripHeadsign);
		if (matcherVIA.find()) {
			String gTripHeadsignBeforeVIA = tripHeadsign.substring(0, matcherVIA.start());
			tripHeadsign = gTripHeadsignBeforeVIA;
		}
		tripHeadsign = CLEAN_STATION.matcher(tripHeadsign).replaceAll(CLEAN_STATION_REPLACEMENT);
		tripHeadsign = MUSEE_CANADIEN_HISTOIRE.matcher(tripHeadsign).replaceAll(MUSEE_CANADIEN_HISTOIRE_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_ET.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_ET_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanSlashes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	private static final Pattern ENDS_WITH_BOUNDS = Pattern.compile("((dir )?(est|ouest|nord|sud)$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_ENDS_WITH_ARRIVAL_DEPARTURE = Pattern.compile("(" + "^(arriv[e|é]e|d[e|é]part)" + "|"
			+ "(arr[e|ê]t d'arriv[e|é]e|arriv[e|é]e|d[e|é]part)$" + ")", Pattern.CASE_INSENSITIVE);

	private static final Pattern CLEAN_ARRET_DE_COURTOISIE = Pattern.compile("((arr[e|ê|Ê]t de courtoisie[\\s]*)(.*))", Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_ARRET_DE_COURTOISIE_REPLACEMENT = "$3 (Arrêt de Courtoisie)";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		gStopName = ENDS_WITH_BOUNDS.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = STARTS_ENDS_WITH_ARRIVAL_DEPARTURE.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CLEAN_ARRET_DE_COURTOISIE.matcher(gStopName).replaceAll(CLEAN_ARRET_DE_COURTOISIE_REPLACEMENT);
		gStopName = CleanUtils.SAINT.matcher(gStopName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_ET.matcher(gStopName).replaceAll(CleanUtils.CLEAN_ET_REPLACEMENT);
		gStopName = CleanUtils.cleanSlashes(gStopName);
		gStopName = CleanUtils.removePoints(gStopName);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@Override
	public String getStopCode(GStop gStop) {
		if (StringUtils.isEmpty(gStop.getStopCode())) {
			return String.valueOf(gStop.getStopId()); // use stop ID as stop code
		}
		return super.getStopCode(gStop);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(GStop gStop) {
		if (!Utils.isDigitsOnly(gStop.getStopId())) {
			Matcher matcher = DIGITS.matcher(gStop.getStopId());
			if (matcher.find()) {
				int digits = Integer.parseInt(matcher.group());
				if (gStop.getStopId().toLowerCase(Locale.ENGLISH).endsWith("a")) {
					return 100000 + digits;
				}
			}
			System.out.printf("\nUnexpected stop ID for %s!\n", gStop);
			System.exit(-1);
			return -1;
		}
		return super.getStopId(gStop);
	}
}
