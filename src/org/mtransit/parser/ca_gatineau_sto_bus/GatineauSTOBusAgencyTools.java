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
		if (gTrip.getTripHeadsign().equalsIgnoreCase("En Transit")) {
			return true;
		}
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		return super.excludeRoute(gRoute);
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
		return cleanRouteLongName(gRoute);
	}

	private String cleanRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		routeLongName = CEGEP_GABRIELLE_ROY_.matcher(routeLongName).replaceAll(CEGEP_GABRIELLE_ROY_REPLACEMENT);
		routeLongName = CleanUtils.cleanSlashes(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	@Override
	public boolean mergeRouteLongName(MRoute mRoute, MRoute mRouteToMerge) {
		if (mRoute.simpleMergeLongName(mRouteToMerge)) {
			return super.mergeRouteLongName(mRoute, mRouteToMerge);
		}
		List<String> routeLongNamess = Arrays.asList(mRoute.getLongName(), mRouteToMerge.getLongName());
		if (mRoute.getId() == 33L) {
			if (Arrays.asList( //
					"Station Cité / G-Roy / Ottawa", //
					"Station Cité / Cegep G-Roy / Ottawa", //
					"Station De La Cité / Cegep Gabrielle-Roy / Ottawa" //
			).containsAll(routeLongNamess)) {
				mRoute.setLongName("Station De La Cité / Cegep Gabrielle-Roy / Ottawa");
				return true;
			}
		} else if (mRoute.getId() == 37L) {
			if (Arrays.asList( //
					"Cegep G-Roy", //
					"Cegep Gabrielle-Roy" //
			).containsAll(routeLongNamess)) {
				mRoute.setLongName("Cegep Gabrielle-Roy");
				return true;
			}
			if (Arrays.asList( //
					"Cegep G-Roy", //
					"Cegep Gabrielle-Roy", //
					"Cegep Gab-Roy / St-Joseph" //
			).containsAll(routeLongNamess)) {
				mRoute.setLongName("Cegep Gab-Roy / St-Joseph");
				return true;
			}
		} else if (mRoute.getId() == 66L) {
			if (Arrays.asList( //
					"Mont-Luc / Dubarry", //
					"Montluc Dubarry" //
			).containsAll(routeLongNamess)) {
				mRoute.setLongName("Mont-Luc / Dubarry");
				return true;
			}
		} else if (mRoute.getId() == 88L) {
			if (Arrays.asList( //
					"Station Labrosse / Cheval-Blanc", //
					"Station Labrosse-Cheval Blanc" //
			).containsAll(routeLongNamess)) {
				mRoute.setLongName("Station Labrosse / Cheval-Blanc");
				return true;
			}
		}
		if (isGoodEnoughAccepted()) {
			return super.mergeRouteLongName(mRoute, mRouteToMerge);
		}
		System.out.printf("\nUnexpected routes to merge: %s & %s!\n", mRoute, mRouteToMerge);
		System.exit(-1);
		return false;
	}

	@SuppressWarnings("unused")
	private static final String AGENCY_COLOR_GREEN = "33A949"; // GREEN PANTONE 360 / 361 (90%) (from Corporate Logo Usage PDF)
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
	private static final String SCHOOL_BUS_COLOR = "FFD800"; // YELLOW (from Wikipedia)

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 11: return PEAK_COLOR;
			case 15: return PEAK_COLOR;
			case 17: return PEAK_COLOR;
			case 18: return null; // TODO ?
			case 20: return PEAK_COLOR;
			case 21: return REGULAR_COLOR;
			case 22: return PEAK_COLOR;
			case 23: return null; // TODO ?
			case 24: return PEAK_COLOR;
			case 25: return PEAK_COLOR;
			case 26: return PEAK_COLOR;
			case 27: return PEAK_COLOR;
			case 28: return PEAK_COLOR;
			case 29: return PEAK_COLOR;
			case 31: return REGULAR_COLOR; // OCCASIONAL_COLOR;
			case 32: return PEAK_COLOR;
			case 33: return REGULAR_COLOR; // OCCASIONAL_COLOR;
			case 34: return null; // TODO ?
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
			case 55: return REGULAR_COLOR;
			case 56: return null; // TODO ?
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
			case 68: return REGULAR_COLOR; // RAPIBUS_COLOR
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
			case 87: return PEAK_COLOR;
			case 88: return PEAK_COLOR;
			case 93: return PEAK_COLOR; // RAPIBUS_COLOR
			case 95: return PEAK_COLOR; // RAPIBUS_COLOR
			case 94: return PEAK_COLOR;
			case 97: return REGULAR_COLOR;
			case 98: return PEAK_COLOR;
			case 100: return RB100_COLOR; // RAPIBUS_COLOR
			case 200: return RB200_COLOR; // RAPIBUS_COLOR
			case 300: return REGULAR_COLOR; // RAPIBUS_COLOR
			case 325: return SCHOOL_BUS_COLOR;
			case 327: return SCHOOL_BUS_COLOR;
			case 331: return SCHOOL_BUS_COLOR;
			case 333: return SCHOOL_BUS_COLOR;
			case 338: return SCHOOL_BUS_COLOR;
			case 339: return SCHOOL_BUS_COLOR;
			case 371: return null; // TODO ?
			case 400: return REGULAR_COLOR; // RAPIBUS_COLOR
			case 439: return SCHOOL_BUS_COLOR;
			case 472: return null; // TODO ?
			case 500: return REGULAR_COLOR; // RAPIBUS_COLOR
			case 533: return SCHOOL_BUS_COLOR;
			case 539: return SCHOOL_BUS_COLOR;
			case 564: return SCHOOL_BUS_COLOR;
			case 625: return SCHOOL_BUS_COLOR;
			case 627: return SCHOOL_BUS_COLOR;
			case 629: return SCHOOL_BUS_COLOR;
			case 633: return SCHOOL_BUS_COLOR;
			case 637: return SCHOOL_BUS_COLOR;
			case 649: return SCHOOL_BUS_COLOR;
			case 650: return SCHOOL_BUS_COLOR;
			case 651: return SCHOOL_BUS_COLOR;
			case 653: return SCHOOL_BUS_COLOR;
			case 654: return SCHOOL_BUS_COLOR;
			case 666: return SCHOOL_BUS_COLOR;
			case 671: return SCHOOL_BUS_COLOR;
			case 676: return SCHOOL_BUS_COLOR;
			case 696: return SCHOOL_BUS_COLOR;
			case 731: return SCHOOL_BUS_COLOR;
			case 733: return SCHOOL_BUS_COLOR;
			case 735: return SCHOOL_BUS_COLOR;
			case 737: return SCHOOL_BUS_COLOR;
			case 739: return SCHOOL_BUS_COLOR;
			case 740: return SCHOOL_BUS_COLOR;
			case 749: return SCHOOL_BUS_COLOR;
			case 751: return SCHOOL_BUS_COLOR;
			case 753: return SCHOOL_BUS_COLOR;
			case 754: return SCHOOL_BUS_COLOR;
			case 767: return SCHOOL_BUS_COLOR;
			case 800: return PEAK_COLOR; // RAPIBUS_COLOR
			case 804: return null; // TODO
			case 805: return null; // TODO
			case 807: return null; // TODO ?
			case 810: return PEAK_COLOR; // RAPIBUS_COLOR
			case 811: return null; // TODO ?
			case 813: return null; // TODO
			case 824: return null; // TODO
			case 829: return SCHOOL_BUS_COLOR;
			case 839: return SCHOOL_BUS_COLOR;
			case 849: return SCHOOL_BUS_COLOR;
			case 850: return SCHOOL_BUS_COLOR;
			case 859: return null; // TODO
			case 870: return PEAK_COLOR; // RAPIBUS_COLOR // TODO ??
			case 873: return null; // TODO
			case 901: return null;
			case 904: return null; // TODO ?
			case 950: return null; // TODO
			// @formatter:on
			}
			if (isGoodEnoughAccepted()) {
				return null;
			}
			System.out.printf("\nUnexpected route color %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return super.getRouteColor(gRoute);
	}

	private static final String STATION_ = ""; // "Ston ";
	private static final String LABROSSE = "Labrosse";
	private static final String LABROSSE_STATION = STATION_ + LABROSSE;
	private static final String MUSEE_CANADIEN_HISTOIRE_SHORT = "Musée de l'Histoire";
	private static final String FREEMAN = "Freeman";
	private static final String OTTAWA = "Ottawa";
	private static final String PLACE_D_ACCUEIL = "Pl.Accueil"; // "Place d'Accueil";
	private static final String DE_LA_CITÉ = "Cité"; // De La
	private static final String LORRAIN = "Lorrain";
	private static final String RIVERMEAD = "Rivermead";
	private static final String LES_PROMENADES = "Les Promenades";
	private static final String P_O_B_SHORT = "P-O-B";
	private static final String P_O_B_ALLUM = P_O_B_SHORT + " Allum";
	private static final String P_O_B_FREEMAN = P_O_B_SHORT + " " + FREEMAN;
	private static final String P_O_B_LES_PROMENDADES = P_O_B_SHORT + " " + LES_PROMENADES;
	private static final String DE_LA_GALÈNE = "Galène"; // De La
	private static final String PLATEAU = "Plateau";
	private static final String TERRASSES = "Tsses";
	private static final String TERRASSES_DE_LA_CHAUDIERE = TERRASSES + " de la Chaudière";
	private static final String PARC_CHAMPLAIN = "Parc Champlain";
	private static final String MONT_LUC = "Mont-Luc";
	private static final String MASSON_ANGERS = "Masson-Angers";
	private static final String CEGEP_GABRIELLE_ROY_SHORT = "Cgp GRoy";
	private static final String COLLEGE_SAINT_ALEXANDRE_SHORT = "Col St-Alex";
	private static final String COLLEGE_SAINT_JOSEPH_SHORT = "Col St-Jo";
	private static final String COLLEGE_NOUVELLES_FRONTIERES_SHORT = "Col NF";
	private static final String ECOLE_SECONDAIRE_DE_L_ILE_SHORT = "ES De L'Île";
	private static final String ECOLE_SECONDAIRE_GRANDE_RIVIERE = "ES G Rivière";
	private static final String ECOLE_SECONDAIRE_MONT_BLEU = "ES Mont-Bleu";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(79L, new RouteTripSpec(79L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LORRAIN, // St-Thomas
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LABROSSE_STATION) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3997", // Quai local Labrosse
								"3744", // ++
								"4476", // LORRAIN/des POMMETIERS est
								"4482", // == LORRAIN/BLANCHETTE est
								"4484", // !=
								"4512", // != de CHAMBORD/LORRAIN
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"4512", // != de CHAMBORD/LORRAIN
								"4167", // LORRAIN/des FLEURS ouest
								"3746", // ++
								"8502" // Terminus ligne 79
						})) //
				.compileBothTripSort());
		map2.put(325L, new RouteTripSpec(325L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_SAINT_ALEXANDRE_SHORT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, PLATEAU) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2767", // PINK/de la SAPINIÈRE nord
								"2273", // du PLATEAU/SAINT-RAYMOND sud
								"3440", // SAINT-LOUIS/LEBAUDY est
								"9603", // ÉCOLE SAINT-ALEXANDRE
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"9603", // ÉCOLE SAINT-ALEXANDRE
								"3442", // SAINT-LOUIS/LEBAUDY ouest
								"2767", // PINK/de la SAPINIÈRE nord
								"2273", // du PLATEAU/SAINT-RAYMOND sud
						})) //
				.compileBothTripSort());
		map2.put(327L, new RouteTripSpec(327L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_SAINT_ALEXANDRE_SHORT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Hautes-Plaines") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2777", // MARIE-BURGER/de la GALÈNE est
								"3440", // SAINT-LOUIS/LEBAUDY est
								"9603", // ÉCOLE SAINT-ALEXANDRE
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"9603", // ÉCOLE SAINT-ALEXANDRE
								"3442", // SAINT-LOUIS/LEBAUDY ouest
								"2653", // de la GALÈNE/des MINEURS ouest
						})) //
				.compileBothTripSort());
		map2.put(333L, new RouteTripSpec(333L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_SAINT_ALEXANDRE_SHORT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, FREEMAN) // CEGEP_GABRIELLE_ROY_SHORT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2015", // CEGEP GABRIELLE-ROY #
								"3440", // SAINT-LOUIS/LEBAUDY est
								"9603", // ÉCOLE SAINT-ALEXANDRE
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"9603", // ÉCOLE SAINT-ALEXANDRE
								"3442", // SAINT-LOUIS/LEBAUDY ouest
								"2153", // TERMINUS FREEMAN
						})) //
				.compileBothTripSort());
		map2.put(339L, new RouteTripSpec(339L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_SAINT_ALEXANDRE_SHORT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, PLATEAU) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2602", // TERRASSES de la CHAUDIÈRE nord
								"2004", // ALEXANDRE-TACHÉ/SAINT-RAYMOND sud
								"2239", // du PLATEAU/des CÈDRES
								"3440", // SAINT-LOUIS/LEBAUDY est
								"9603", // ÉCOLE SAINT-ALEXANDRE
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"9603", // ÉCOLE SAINT-ALEXANDRE
								"3442", // SAINT-LOUIS/LEBAUDY ouest
								"2604", // TERRASSES de la CHAUDIÈRE sud
						})) //
				.compileBothTripSort());
		map2.put(439l, new RouteTripSpec(439l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PLATEAU) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2271", // du PLATEAU/SAINT-RAYMOND nord
								"2642" // SAINT-RÉDEMPTEUR/SACRÉ-COEUR ouest
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR est
								"2604" // TERRASSES de la CHAUDIÈRE sud
						})) //
				.compileBothTripSort());
		map2.put(533L, new RouteTripSpec(533L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LES_PROMENADES, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Cité des jeunes") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2422", // LIONEL-ÉMOND/SAINT-RAYMOND
								"3003", // LES PROMENADES
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3003", // LES PROMENADES
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(539L, new RouteTripSpec(539L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT, // "Émond / Gamelin"
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_SAINT_JOSEPH_SHORT) // "Taché / St-Joseph"
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2427", // LIONEL-ÉMOND/GAMELIN ouest
								"2064", // ALEXANDRE-TACHÉ/SAINT-JOSEPH sud
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2065", // ALEXANDRE-TACHÉ/SAINT-JOSEPH nord
								"2239", // du PLATEAU/des CÈDRES nord
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(564L, new RouteTripSpec(564L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LES_PROMENADES, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2422", // LIONEL-ÉMOND/SAINT-RAYMOND
								"3003", // LES PROMENADES
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3000", // LES PROMENADES
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(625L, new RouteTripSpec(625L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Pink / Conservatoire") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1459", // du CONSERVATOIRE/du LOUVRE ouest
								"2642" // SAINT-RÉDEMPTEUR/SACRÉ-COEUR ouest
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR est
								"1460" // du CONSERVATOIRE/du LOUVRE est
						})) //
				.compileBothTripSort());
		map2.put(629L, new RouteTripSpec(629L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Le Manoir") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2763", // des TREMBLES/des GRIVES ouest
								"2006", // ALEXANDRE-TACHÉ/SAINT-DOMINIQUE sud
								"2025", //
								"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR est
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2642", // SAINT-RÉDEMPTEUR/SACRÉ-COEUR ouest
								"2506", //
								"2775", // LOUISE-CAMPAGNA/SAINT-RAYMOND nord
								"2763", // des TREMBLES/des GRIVES ouest
						})) //
				.compileBothTripSort());
		map2.put(633l, new RouteTripSpec(633l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, FREEMAN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2644", "2015", "2151" //
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2153", "2642" //
						})) //
				.compileBothTripSort());
		map2.put(637L, new RouteTripSpec(637L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "St-Joseph") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2006", // ALEXANDRE-TACHÉ/SAINT-DOMINIQUE sud
								"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR est
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2642", // SAINT-RÉDEMPTEUR/SACRÉ-COEUR ouest
								"2004", // ALEXANDRE-TACHÉ/SAINT-RAYMOND sud
						})) //
				.compileBothTripSort());
		map2.put(649L, new RouteTripSpec(649L, //
				0, MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_MONT_BLEU, //
				1, MTrip.HEADSIGN_TYPE_STRING, RIVERMEAD) // PLATEAU
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1377", // PARC-O-BUS RIVERMEAD
								"2188", // de la CITÉ-DES-JEUNES/TALBOT
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"2192", // de la CITÉ-DES-JEUNES/TALBOT
								"1171", // Parc-O-Bus RIVERMEAD arrivée RIVERMEAD
						})) //
				.compileBothTripSort());
		map2.put(650L, new RouteTripSpec(650L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Pink / Conservatoire", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1124", // BROAD/ANNA est
								"1355", // !=
								"1377", // <> PARC-O-BUS RIVERMEAD ouest
								"1307", // !=
								"1403", // PINK/du CONSERVATOIRE sud
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1460", // du CONSERVATOIRE/du LOUVRE
								"1306", // !=
								"1377", // <> PARC-O-BUS RIVERMEAD ouest
								"1352", // !=
								"1226", // PRINCIPALE/du BORDEAUX nord
								"1129", // BROAD/LOUIS-SAINT-LAURENT ouest
								"9037", // FICTIF GRANDE RIVIÈRE
						})) //
				.compileBothTripSort());
		map2.put(651L, new RouteTripSpec(651L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERMEAD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"9036", // FICTIF GRANDE RIVIERE
								"1128", // BROAD/LOUIS-SAINT-LAURENT est
								"1171", // Parc-O-Bus RIVERMEAD arrivée RIVERMEAD
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1377", // PARC-O-BUS RIVERMEAD ouest
								"1131", // BAGOT/BROAD sud
								"9037", // FICTIF GRANDE RIVIÈRE
						})) //
				.compileBothTripSort());
		map2.put(653L, new RouteTripSpec(653L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERMEAD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"9036", // FICTIF GRANDE RIVIERE
								"1128", // BROAD/LOUIS-SAINT-LAURENT est
								"1358", // chemin d'AYLMER/RIVERMEAD sud
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1377", // PARC-O-BUS RIVERMEAD ouest
								"1075", // ++
								"9036", // FICTIF GRANDE RIVIERE
						})) //
				.compileBothTripSort());
		map2.put(654L, new RouteTripSpec(654L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PARC_CHAMPLAIN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"9036", // FICTIF GRANDE RIVIERE
								"1128", // BROAD/LOUIS-SAINT-LAURENT est
								"1281", // d'HOCHELAGA/ATHOLL DOUNE nord
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1365", // d'HOCHELAGA/ATHOLL DOUNE nord
								"1129", // BROAD/LOUIS-SAINT-LAURENT ouest
								"9037", // FICTIF GRANDE RIVIÈRE
						})) //
				.compileBothTripSort());
		map2.put(666L, new RouteTripSpec(666L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MONT_LUC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2422", // LIONEL-ÉMOND/SAINT-RAYMOND est
								"3446", // ++
								"4351", // de CANNES/de CAVAILLON ouest
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"4352", // de CANNES/de CAVAILLON est
								"4319", // ++
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(671L, new RouteTripSpec(671L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LABROSSE_STATION, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2422", // LIONEL-ÉMOND/SAINT-RAYMOND est
								"3666", // ++
								"3990", // Terminus Labrosse
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3991", // Quai local LABROSSE #5
								"8061", // == Station de la GAPPE #1
								"2553", // != de la CARRIÈRE/d'EDMONTON
								"2549", // != de la CARRIÈRE/du CASINO
								"8051", // != Station LAC LEAMY
								"2114", // != SAINT-JOSEPH/GAMELIN
								"2404", // == SAINT-RAYMOND/ROY
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(676L, new RouteTripSpec(676L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LABROSSE_STATION, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2427", // LIONEL-ÉMOND/GAMELIN ouest
								"3990", // Terminus Labrosse
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3991", // Quai local LABROSSE #5
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(696L, new RouteTripSpec(696L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MASSON_ANGERS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2422", // LIONEL-ÉMOND/SAINT-RAYMOND est
								"4773", // de l'ARÉNA/LOMBARD arrivée
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"4772", // de l'ARÉNA/LOMBARD nord
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(731L, new RouteTripSpec(731L, //
				0, MTrip.HEADSIGN_TYPE_STRING, CEGEP_GABRIELLE_ROY_SHORT, //
				1, MTrip.HEADSIGN_TYPE_STRING, "E Montbleu") //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"2682", // LAURIER/des ALLUMETTIÈRES
								"2650", // SACRÉ-COEUR/SAINT-HENRI
								"2424", // LIONEL-ÉMOND/GAMELIN
								"2008", // CEGEP GABRIELLE-ROY/arrivée
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"2013", // CEGEP GABRIELLE-ROY #3
								"2188", // de la CITÉ-DES-JEUNES/TALBOT
								"2272", // RIEL/ISABELLE
								"2672", // SACRÉ-COEUR/SAINT-RÉDEMPTEUR
								"2680", // LAURIER/des ALLUMETTIÈRES
						})) //
				.compileBothTripSort());
		map2.put(733L, new RouteTripSpec(733L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, P_O_B_FREEMAN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_MONT_BLEU) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2188", // de la CITÉ-DES-JEUNES/TALBOT est
								"2151", // TERMINUS Parc-o-bus FREEMAN
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2153", // TERMINUS FREEMAN
								"2011", // CEGEP GABRIELLE-ROY
						})) //
				.compileBothTripSort());
		map2.put(735l, new RouteTripSpec(735l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Galène / Mineurs", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Émond / Gamelin") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2188", // de la CITÉ-DES-JEUNES/TALBOT est
								"2653", // de la GALÈNE/des MINEURS ouest
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2777", // MARIE-BURGER/de la GALÈNE est
								"2192", // de la CITÉ-DES-JEUNES/TALBOT ouest
								"2427", // LIONEL-ÉMOND/GAMELIN ouest
						})) //
				.compileBothTripSort());
		map2.put(737l, new RouteTripSpec(737l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERRASSES_DE_LA_CHAUDIERE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_MONT_BLEU) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2192", // de la CITÉ-DES-JEUNES/TALBOT ouest
								"2604" // TERRASSES de la CHAUDIÈRE sud
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2602", // TERRASSES de la CHAUDIÈRE nord
								"2120", // SAINT-JOSEPH/RENÉ-MARENGÈRE est
								"2188" // de la CITÉ-DES-JEUNES/TALBOT est
						})) //
				.compileBothTripSort());
		map2.put(739l, new RouteTripSpec(739l, //
				0, MTrip.HEADSIGN_TYPE_STRING, COLLEGE_SAINT_JOSEPH_SHORT, //
				1, MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR
								"2604", // TERRASSES de la CHAUDIÈRE
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"2621", // LAURIER/ÉLISABETH-BRUYÈRE
								"2741", // SAINT-RAYMOND/LOUISE-CAMPAGNA
								"2427", // LIONEL-ÉMOND/GAMELIN
								"2642", // SAINT-RÉDEMPTEUR/SACRÉ-COEUR
						})) //
				.compileBothTripSort());
		map2.put(740L, new RouteTripSpec(740L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_SAINT_JOSEPH_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Jardins-Lavigne") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1108", // FRONT/des ALLUMETTIÈRES est
								"1441", // == KLOCK/de la CLÉMATITE
								"1437", // != MAURICE-DUPLESSIS/KLOCK
								"1425", // != MAURICE-DUPLESSIS/WILFRID-LAVIGNE
								"1453", // != KLOCK/MAURICE-DUPLESSIS
								"1123", // != WILFRID-LAVIGNE/MAURICE-DUPLESSIS
								"1426", // == WILFRID-LAVIGNE/JEAN-LESAGE
								"2604", // TERRASSES de la CHAUDIÈRE sud
								"2615", // LAVAL/LAURIER
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2612", // de l'HÔTEL-de-VILLE/du PORTAGE nord
								"1282", // ++
								"1109", // FRONT/des ALLUMETTIÈRES ouest
						})) //
				.compileBothTripSort());
		map2.put(749L, new RouteTripSpec(749L, //
				0, MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_MONT_BLEU, //
				1, MTrip.HEADSIGN_TYPE_STRING, RIVERMEAD) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1459", // du CONSERVATOIRE/du LOUVRE
								"2188", // de la CITÉ-DES-JEUNES/TALBOT
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"2192", // de la CITÉ-DES-JEUNES/TALBOT
								"2806", // du PLATEAU/du MARIGOT
								"1171", // Parc-O-Bus RIVERMEAD arrivée RIVERMEAD
						})) //
				.compileBothTripSort());
		map2.put(751l, new RouteTripSpec(751l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERMEAD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"9036", // FICTIF GRANDE RIVIERE
								"1124", // BROAD/ANNA est
								"1358", // chemin d'AYLMER/RIVERMEAD sud
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1290", // chemin d'AYLMER/GRIMES nord
								"1078", // WILFRID-LAVIGNE/JOHN-EGAN est
								"9036", // FICTIF GRANDE RIVIERE
						})) //
				.compileBothTripSort());
		map2.put(753l, new RouteTripSpec(753l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Lucerne / Robert-Sterward", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"9036", // FICTIF GRANDE RIVIERE
								"1128", // ++
								"1073", // ++
								"1298", // de LUCERNE/ROBERT-STEWARD
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1298", // de LUCERNE/ROBERT-STEWARD
								"1075", // ++
								"1082", // ++
								"9036", // FICTIF GRANDE RIVIERE
						})) //
				.compileBothTripSort());
		map2.put(754l, new RouteTripSpec(754l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "McConnell / Vanier", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1124", // BROAD/ANNA est
								"1347", // MC CONNELL/MORLEY-WALTERS nord
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1333", // MC CONNELL/MORLEY-WALTERS sud
								"1075", // WILFRID-LAVIGNE/LEGUERRIER
								"1078", // WILFRID-LAVIGNE/JOHN-EGAN est
								"9036", // FICTIF GRANDE RIVIERE
						})) //
				.compileBothTripSort());
		map2.put(767l, new RouteTripSpec(767l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, P_O_B_LES_PROMENDADES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR est
								"3066", // GRÉBER/de la SAVANE est
								"3000", // LES PROMENADES
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2714", // FOURNIER/JOANISSE ouest
								"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR est
						})) //
				.compileBothTripSort());
		map2.put(813L, new RouteTripSpec(813L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Robert Guertin Ctr", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MUSEE_CANADIEN_HISTOIRE_SHORT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2618", // MUSÉE DE L'HISTOIRE
								"2643", // SAINT-RÉDEMPTEUR/ALLARD
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2643", // SAINT-RÉDEMPTEUR/ALLARD
								"2618", // MUSÉE DE L'HISTOIRE
						})) //
				.compileBothTripSort());
		map2.put(839l, new RouteTripSpec(839l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_MONT_BLEU, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Taché / St-Joseph") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2004", // ALEXANDRE-TACHÉ/SAINT-RAYMOND sud
								"2239", // du PLATEAU/des CÈDRES nord
								"2188", // de la CITÉ-DES-JEUNES/TALBOT est
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2192", // de la CITÉ-DES-JEUNES/TALBOT ouest
								"2006", // ALEXANDRE-TACHÉ/SAINT-DOMINIQUE sud
								"2064", // ALEXANDRE-TACHÉ/SAINT-JOSEPH sud
						})) //
				.compileBothTripSort());
		map2.put(870l, new RouteTripSpec(870l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Arena Guertin", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Laurier / Allumettières") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2662", // SACRÉ-COEUR/NOTRE-DAME-de-l'ÎLE
								"2643", // SAINT-RÉDEMPTEUR/ALLARD
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2643", // SAINT-RÉDEMPTEUR/ALLARD
								"2682", // LAURIER/des ALLUMETTIÈRES
						})) //
				.compileBothTripSort());
		map2.put(950L, new RouteTripSpec(950L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Galerie Aylmer", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Front / Harvey") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1040", // FRONT/HARVEY
								"1077", // GALERIES AYLMER arrivée
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1079", // GALERIES AYLMER départ
								"1038", // FRONT/HARVEY
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
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
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 20l) {
			if (Arrays.asList( //
					// "Mhistoire", //
					TERRASSES, //
					OTTAWA //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 21l) {
			if (Arrays.asList( //
					"Casino", //
					"Freeman" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Casino", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Casino", //
					"Ottawa" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Ottawa", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 31l) {
			if (Arrays.asList( //
					CEGEP_GABRIELLE_ROY_SHORT, //
					"Laurier", //
					"Ottawa" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Ottawa", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 32l) {
			if (Arrays.asList( //
					CEGEP_GABRIELLE_ROY_SHORT, //
					"Ed.Lst-Lau" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY_SHORT, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 33l) {
			if (Arrays.asList( //
					"Freeman", //
					DE_LA_CITÉ //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DE_LA_CITÉ, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Mhistoire", //
					"Ottawa", //
					CEGEP_GABRIELLE_ROY_SHORT //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY_SHORT, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 35L) {
			if (Arrays.asList( //
					"H.De-Ville", //
					CEGEP_GABRIELLE_ROY_SHORT, //
					DE_LA_GALÈNE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DE_LA_GALÈNE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 37L) {
			if (Arrays.asList( //
					CEGEP_GABRIELLE_ROY_SHORT, //
					"Laurier", //
					"H.De-Ville" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY_SHORT, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 39l) {
			if (Arrays.asList( //
					PLATEAU, //
					OTTAWA //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 49l) {
			if (Arrays.asList( //
					CEGEP_GABRIELLE_ROY_SHORT, //
					"Gam.Emond" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY_SHORT, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Gal.Aylmer", //
					"Gam.Emond" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Gal.Aylmer", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 50l) {
			if (Arrays.asList( //
					"Gal.Aylmer", //
					P_O_B_ALLUM //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(P_O_B_ALLUM, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 64L) {
			if (Arrays.asList( //
					"Ar Beaudry", //
					FREEMAN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(FREEMAN, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 68L) {
			if (Arrays.asList( //
					LABROSSE, //
					"Affaires" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Affaires", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 87l) {
			if (Arrays.asList( //
					PLACE_D_ACCUEIL, //
					OTTAWA //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PLACE_D_ACCUEIL, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 731l) {
			if (Arrays.asList( //
					CEGEP_GABRIELLE_ROY_SHORT, //
					"Coll St-Jo", //
					"ES De L'Île" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY_SHORT, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"ES De L'Île", //
					"E Montbleu" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("E Montbleu", mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnexpected trips to merge: %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern TO = Pattern.compile("((^|\\W){1}(to)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final Pattern VIA = Pattern.compile("((^|\\W){1}(via)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	private static final Pattern MUSEE_CANADIEN_HISTOIRE_ = Pattern.compile("((^|\\W){1}(mus[e|é]e canadien de l'histoire)(\\W|$){1})",
			Pattern.CASE_INSENSITIVE);
	private static final String MUSEE_CANADIEN_HISTOIRE_REPLACEMENT = "$2" + MUSEE_CANADIEN_HISTOIRE_SHORT + "$4";

	private static final Pattern CLEAN_STATION = Pattern.compile("((^|\\W){1}(station|ston|sta)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_STATION_REPLACEMENT = "$2" + STATION_ + "$4";

	private static final Pattern CEGEP_GABRIELLE_ROY_ = Pattern.compile("((^|\\W){1}(" //
			+ "c[é|É|e|è|È]gep gabrielle\\-roy" //
			+ "|" //
			+ "cegep gab\\.roy" //
			+ "|" //
			+ "cegep gab\\-roy"//
			+ "|" //
			+ "c[é|É|e|è|È]gep groy" //
			+ "|" //
			+ "cegep g\\-roy" //
			+ "|" //
			+ "cgp gabrielle\\-r" //
			+ "|" //
			+ "cgp groy" //
			+ "|" //
			+ "cgp g\\-roy" //
			+ "|" //
			+ "g\\-roy" //
			+ ")(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String CEGEP_GABRIELLE_ROY_REPLACEMENT = "$2" + CEGEP_GABRIELLE_ROY_SHORT + "$4";

	private static final Pattern ECOLE_SECONDAIRE_DE_L_ILE_ = Pattern.compile("((^|\\W){1}([e|é]cole De l'[i|î]le)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String ECOLE_SECONDAIRE_DE_L_ILE_REPLACEMENT = "$2" + ECOLE_SECONDAIRE_DE_L_ILE_SHORT + "$4";

	private static final Pattern COLLEGE_SAINY_ALEXANDRE_ = Pattern.compile("((^|\\W){1}(Col Stalex)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String COLLEGE_SAINY_ALEXANDRE_REPLACEMENT = "$2" + COLLEGE_SAINT_ALEXANDRE_SHORT + "$4";

	private static final Pattern P_O_B = Pattern.compile("((^|\\W){1}(pob|p\\-o\\-b)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String P_O_B_REPLACEMENT = "$2" + P_O_B_SHORT + "$4";

	private static final Pattern PRE_TUNNEY_ = Pattern.compile("((^|\\W){1}(pr[e|é|É] tunney)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String PRE_TUNNEY__REPLACEMENT = "$2" + "Pré-Tunney" + "$4";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
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
		tripHeadsign = CEGEP_GABRIELLE_ROY_.matcher(tripHeadsign).replaceAll(CEGEP_GABRIELLE_ROY_REPLACEMENT);
		tripHeadsign = COLLEGE_SAINY_ALEXANDRE_.matcher(tripHeadsign).replaceAll(COLLEGE_SAINY_ALEXANDRE_REPLACEMENT);
		tripHeadsign = ECOLE_SECONDAIRE_DE_L_ILE_.matcher(tripHeadsign).replaceAll(ECOLE_SECONDAIRE_DE_L_ILE_REPLACEMENT);
		tripHeadsign = P_O_B.matcher(tripHeadsign).replaceAll(P_O_B_REPLACEMENT);
		tripHeadsign = PRE_TUNNEY_.matcher(tripHeadsign).replaceAll(PRE_TUNNEY__REPLACEMENT);
		tripHeadsign = MUSEE_CANADIEN_HISTOIRE_.matcher(tripHeadsign).replaceAll(MUSEE_CANADIEN_HISTOIRE_REPLACEMENT);
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
