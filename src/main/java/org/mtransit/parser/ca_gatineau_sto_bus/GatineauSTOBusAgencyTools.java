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
		System.out.print("\nGenerating STO bus data...");
		long start = System.currentTimeMillis();
		boolean isNext = "next_".equalsIgnoreCase(args[2]);
		if (isNext) {
			setupNext();
		}
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		System.out.printf("\nGenerating STO bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	private void setupNext() {
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
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
					"Station Cité" + _SLASH_ + "G-Roy" + _SLASH_ + "Ottawa", //
					"Station Cité" + _SLASH_ + "Cegep G-Roy" + _SLASH_ + "Ottawa", //
					"Station De La Cité" + _SLASH_ + "Cegep Gabrielle-Roy" + _SLASH_ + "Ottawa" //
			).containsAll(routeLongNamess)) {
				mRoute.setLongName("Station De La Cité" + _SLASH_ + "Cegep Gabrielle-Roy" + _SLASH_ + "Ottawa");
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
					"Cegep Gab-Roy" + _SLASH_ + "St-Joseph" //
			).containsAll(routeLongNamess)) {
				mRoute.setLongName("Cegep Gab-Roy" + _SLASH_ + "St-Joseph");
				return true;
			}
		} else if (mRoute.getId() == 66L) {
			if (Arrays.asList( //
					"Mont-Luc" + _SLASH_ + "Dubarry", //
					"Montluc Dubarry" //
			).containsAll(routeLongNamess)) {
				mRoute.setLongName("Mont-Luc" + _SLASH_ + "Dubarry");
				return true;
			}
		} else if (mRoute.getId() == 88L) {
			if (Arrays.asList( //
					"Station Labrosse" + _SLASH_ + "Cheval-Blanc", //
					"Station Labrosse-Cheval Blanc" //
			).containsAll(routeLongNamess)) {
				mRoute.setLongName("Station Labrosse" + _SLASH_ + "Cheval-Blanc");
				return true;
			}
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
			case 334: return null; // TODO ?
			case 338: return SCHOOL_BUS_COLOR;
			case 339: return SCHOOL_BUS_COLOR;
			case 371: return null; // TODO ?
			case 400: return REGULAR_COLOR; // RAPIBUS_COLOR
			case 434: return null; // TODO ?
			case 439: return SCHOOL_BUS_COLOR;
			case 472: return null; // TODO ?
			case 500: return REGULAR_COLOR; // RAPIBUS_COLOR
			case 533: return SCHOOL_BUS_COLOR;
			case 534: return null; // TODO ?
			case 539: return SCHOOL_BUS_COLOR;
			case 549: return null; // TODO ?
			case 550: return null; // TODO ?
			case 564: return SCHOOL_BUS_COLOR;
			case 566: return null; // TODO ?
			case 571: return null; // TODO ?
			case 576: return null; // TODO ?
			case 597: return null; // TODO ?
			case 625: return SCHOOL_BUS_COLOR;
			case 627: return SCHOOL_BUS_COLOR;
			case 629: return SCHOOL_BUS_COLOR;
			case 633: return SCHOOL_BUS_COLOR;
			case 637: return SCHOOL_BUS_COLOR;
			case 649: return SCHOOL_BUS_COLOR;
			case 650: return SCHOOL_BUS_COLOR;
			case 651: return SCHOOL_BUS_COLOR;
			case 652: return null; // TODO ?
			case 653: return SCHOOL_BUS_COLOR;
			case 654: return SCHOOL_BUS_COLOR;
			case 666: return SCHOOL_BUS_COLOR;
			case 671: return SCHOOL_BUS_COLOR;
			case 676: return SCHOOL_BUS_COLOR;
			case 696: return SCHOOL_BUS_COLOR;
			case 731: return SCHOOL_BUS_COLOR;
			case 733: return SCHOOL_BUS_COLOR;
			case 734: return null; // TODO ?
			case 735: return SCHOOL_BUS_COLOR;
			case 737: return SCHOOL_BUS_COLOR;
			case 739: return SCHOOL_BUS_COLOR;
			case 740: return SCHOOL_BUS_COLOR;
			case 749: return SCHOOL_BUS_COLOR;
			case 750: return null; // TODO ?
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
			case 825: return null; // TODO ?
			case 827: return null; // TODO ?
			case 829: return SCHOOL_BUS_COLOR;
			case 831: return null; // TODO ?
			case 833: return null; // TODO ?
			case 834: return null; // TODO ?
			case 837: return null; // TODO ?
			case 839: return SCHOOL_BUS_COLOR;
			case 849: return SCHOOL_BUS_COLOR;
			case 850: return SCHOOL_BUS_COLOR;
			case 859: return null; // TODO
			case 867: return null; // TODO ?
			case 870: return PEAK_COLOR; // RAPIBUS_COLOR // TODO ??
			case 873: return null; // TODO
			case 901: return null;
			case 904: return null; // TODO ?
			case 929: return null; // TODO ?
			case 931: return null; // TODO ?
			case 932: return null; // TODO ?
			case 933: return null; // TODO ?
			case 934: return null; // TODO ?
			case 935: return null; // TODO ?
			case 937: return null; // TODO ?
			case 949: return null; // TODO ?
			case 950: return null; // TODO ?
			case 990: return null; // TODO ?
			// @formatter:on
			}
			System.out.printf("\nUnexpected route color %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return super.getRouteColor(gRoute);
	}

	private static final String _SLASH_ = " / ";
	private static final String STATION_ = ""; // "Ston ";
	private static final String ASTICOU_CENTER = "Asticou Ctr";
	private static final String ARENA_BEAUDRY = "Ar Beaudry";
	private static final String LABROSSE = "Labrosse";
	private static final String LABROSSE_STATION = STATION_ + LABROSSE;
	private static final String LAURIER = "Laurier";
	private static final String MUSEE_CANADIEN_HISTOIRE_SHORT = "Musée de l'Histoire";
	private static final String FREEMAN = "Freeman";
	private static final String OTTAWA = "Ottawa";
	private static final String PLACE_D_ACCUEIL = "Pl.Accueil"; // "Place d'Accueil";
	private static final String DE_LA_CITÉ = "Cité"; // De La
	private static final String LORRAIN = "Lorrain";
	private static final String RIVERMEAD = "Rivermead";
	private static final String PROMENADES = "Promenades";
	private static final String LES_PROMENADES = "Les " + PROMENADES;
	private static final String ALLUMETTIERES_SHORT = "Allum";
	private static final String COTES_DES_NEIGES = "Côtes-Des-Neiges";
	private static final String P_O_B_SHORT = "P-O-B";
	private static final String P_O_B_ALLUMETTIERES = P_O_B_SHORT + " " + ALLUMETTIERES_SHORT;
	private static final String P_O_B_LES_PROMENDADES = P_O_B_SHORT + " " + LES_PROMENADES;
	private static final String P_O_B_LORRAIN = P_O_B_SHORT + " " + LORRAIN;
	private static final String PLATEAU = "Plateau";
	private static final String TERRASSES = "Tsses";
	private static final String TERRASSES_DE_LA_CHAUDIERE = TERRASSES + " Chaudière";
	private static final String PARC_CHAMPLAIN = "Parc Champlain";
	private static final String PARC_LA_BAIE = "Parc La Baie";
	private static final String MONT_LUC = "Mont-Luc";
	private static final String MASSON_ANGERS = "Masson-Angers";
	private static final String CEGEP_GABRIELLE_ROY_SHORT = "Cgp GRoy";
	private static final String COLLEGE_SHORT = "Col";
	private static final String COLLEGE_SAINT_ALEXANDRE_SHORT = COLLEGE_SHORT + " St-Alex";
	private static final String COLLEGE_SAINT_JOSEPH_SHORT = COLLEGE_SHORT + " St-Jo";
	private static final String COLLEGE_NOUVELLES_FRONTIERES_SHORT = COLLEGE_SHORT + " NF";
	private static final String ECOLE_SECONDAIRE_DE_L_ILE_SHORT = "ES De L'Île";
	private static final String LAVIGNE = "Lavigne";
	private static final String JARDINS_LAVIGNE_SHORT = "J" + LAVIGNE;
	private static final String ECOLE_SECONDAIRE_GRANDE_RIVIERE = "ES G Rivière";
	private static final String ECOLE_SECONDAIRE_MONT_BLEU = "ES Mont-Bleu";
	private static final String GALERIES_AYLMER_SHORT = "Gal.Aylmer";
	private static final String GAMELIN_EMOND = "Gam.Emond";
	private static final String FRONT = "Front";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(325L, new RouteTripSpec(325L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_SAINT_ALEXANDRE_SHORT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, PLATEAU) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2767", // PINK/de la SAPINIÈRE
								"2273", // du PLATEAU/SAINT-RAYMOND sud
								"3440", // SAINT-LOUIS/LEBAUDY est
								"3334", // SAINT-LOUIS/LEBAUDY #StAlex
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"9603", // ÉCOLE SAINT-ALEXANDRE
								"3442", // SAINT-LOUIS/LEBAUDY ouest
								"5767", // PINK/de la SAPINIÈRE
								"5273", // du PLATEAU/ SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(327L, new RouteTripSpec(327L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_SAINT_ALEXANDRE_SHORT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Galène" + _SLASH_ + "Mineurs") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2777", // MARIE-BURGER/de la GALÈNE est
								"3440", // SAINT-LOUIS/LEBAUDY est
								"3334", // SAINT-LOUIS/LEBAUDY #StAlex
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"9603", // ÉCOLE SAINT-ALEXANDRE #StAlex
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
								"3334", // SAINT-LOUIS/LEBAUDY #StAlex
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"9603", // ÉCOLE SAINT-ALEXANDRE
								"3442", // SAINT-LOUIS/LEBAUDY ouest
								"2153", // TERMINUS FREEMAN
						})) //
				.compileBothTripSort());
		map2.put(334L, new RouteTripSpec(334L, //
				0, MTrip.HEADSIGN_TYPE_STRING, COLLEGE_SAINT_ALEXANDRE_SHORT, //
				1, MTrip.HEADSIGN_TYPE_STRING, PLATEAU) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"2602", // TERRASSES de la CHAUDIÈRE nord
								"2004", // ALEXANDRE-TACHÉ/SAINT-RAYMOND sud
								"2239", // du PLATEAU/des CÈDRES
								"3440", // SAINT-LOUIS/LEBAUDY est
								"3334", // SAINT-LOUIS/LEBAUDY #StAlex
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"9603", // ÉCOLE SAINT-ALEXANDRE
								"3442", // SAINT-LOUIS/LEBAUDY
								"2795", // ++
								"2604", // TERRASSES de la CHAUDIÈRE sud
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
		map2.put(434L, new RouteTripSpec(434L, //
				0, MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT, //
				1, MTrip.HEADSIGN_TYPE_STRING, PLATEAU) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"2271", // du PLATEAU/SAINT-RAYMOND nord
								"2642" // SAINT-RÉDEMPTEUR/SACRÉ-COEUR ouest
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR est
								"2604" // TERRASSES de la CHAUDIÈRE sud
						})) //
				.compileBothTripSort());
		map2.put(439L, new RouteTripSpec(439L, //
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
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DE_LA_CITÉ, // LES_PROMENADES, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2422", // LIONEL-ÉMOND/SAINT-RAYMOND
								"2215", // de la CITÉ-DES-JEUNES/SAINT-RAYMOND
								"3500", // QUAI LOCAL de la CITÉ
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3481", "3483", // QUAI LOCAL de la CITÉ
								"2218", // de la CITÉ-DES-JEUNES/ BÉDARD
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(534L, new RouteTripSpec(534L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT, // "Émond / Gamelin"
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Taché" + _SLASH_ + "St-Joseph") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2065", // ALEXANDRE-TACHÉ/SAINT-JOSEPH
								"2239", // du PLATEAU/des CÈDRES
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2427", // LIONEL-ÉMOND/ GAMELIN
								"2233", // ++
								"2064", // ALEXANDRE-TACHÉ/SAINT-JOSEPH
						})) //
				.compileBothTripSort());
		map2.put(564L, new RouteTripSpec(564L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LES_PROMENADES, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2422", // LIONEL-ÉMOND/SAINT-RAYMOND
								"2286", // SAINT-RAYMOND/ISABELLE
								"3003", // LES PROMENADES
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3000", // LES PROMENADES
								"2288", // SAINT-RAYMOND/CORBEIL
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(566L, new RouteTripSpec(566L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MONT_LUC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2422", // LIONEL-ÉMOND/SAINT-RAYMOND
								"5503", // ++
								"4351", // de CANNES/de CAVAILLON
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"4352", // de CANNES/de CAVAILLON
								"3442", // ++
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(571L, new RouteTripSpec(571L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LABROSSE_STATION, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2422", // LIONEL-ÉMOND/SAINT-RAYMOND
								"3666", // ++
								"3990", // Terminus Labrosse
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3991", "3992", // Quai local LABROSSE
								"3675", // ++
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(576L, new RouteTripSpec(576L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LABROSSE_STATION, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2427", // LIONEL-ÉMOND/ GAMELIN
								"4380", // ++
								"3990", // Terminus Labrosse
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3991", "3992", // Quai local LABROSSE
								"4464", // ++
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(597L, new RouteTripSpec(597L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MASSON_ANGERS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2422", // LIONEL-ÉMOND/SAINT-RAYMOND
								"4723", // ++
								"4773", // de l'ARÉNA/LOMBARD arrivée
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"4772", // de l'ARÉNA/LOMBARD
								"4706", // ++
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(625L, new RouteTripSpec(625L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Pink" + _SLASH_ + "Conservatoire") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1459", // du CONSERVATOIRE/du LOUVRE ouest
								"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2642", // SAINT-RÉDEMPTEUR/SACRÉ-COEUR
								"1460" // du CONSERVATOIRE/du LOUVRE est
						})) //
				.compileBothTripSort());
		map2.put(629L, new RouteTripSpec(629L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Le Manoir") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2775", // LOUISE-CAMPAGNA/SAINT-RAYMOND
								"2763", // des TREMBLES/des GRIVES ouest
								"2541", // !=
								"2002", // <>
								"2006", // <> ALEXANDRE-TACHÉ/SAINT-DOMINIQUE
								"2025", // != ALEXANDRE-TACHÉ/SAINTE-THÉRÈSE
								"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR est #E_S_DE_L_ILE
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2642", // SAINT-RÉDEMPTEUR/SACRÉ-COEUR ouest #E_S_DE_L_ILE
								"2506", // des ALLUMETTIÈRES/LABELLE
								"1775", // LOUISE-CAMPAGNA/SAINT-RAYMOND
								"1541", // != ==
								"2002", // <> !=
								"2006", // <> != ALEXANDRE-TACHÉ/SAINT-DOMINIQUE =>
								"3002", // !=
								"3006", // != ALEXANDRE-TACHÉ/SAINT-DOMINIQUE =>
						})) //
				.compileBothTripSort());
		map2.put(633L, new RouteTripSpec(633L, //
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
				1, MTrip.HEADSIGN_TYPE_STRING, ASTICOU_CENTER) // PLATEAU) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1377", // PARC-O-BUS RIVERMEAD
								"1051", // VANIER/du PLATEAU
								"2243", // ++
								"2273", // ==
								"2215", // !=
								"2089", // CENTRE ASTICOU =>
						// "2285", // != #PLATEAU
						// "2642", // != SAINT-RÉDEMPTEUR/SACRÉ-COEUR #PLATEAU => #PLATEAU
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						// "2644", // != SAINT-RÉDEMPTEUR/SACRÉ-CŒUR #PLATEAU <= #PLATEAU
						// "2287", // != #PLATEAU
								"2210", // != CENTRE ASTICOU <=
								"2218", // !=
								"2766", // ==
								"2245", // ++
								"1171", // Parc-O-Bus RIVERMEAD arrivée RIVERMEAD
						})) //
				.compileBothTripSort());
		map2.put(650L, new RouteTripSpec(650L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Pink" + _SLASH_ + "Conservatoire", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1124", // != BROAD/ANNA est <=
								"9036", // != FICTIF GRANDE RIVIERE-Départ <=
								"1128", // ==
								"1355", // !=
								"2377", // != PARC-O-BUS RIVERMEAD =>
								"1377", // <> PARC-O-BUS RIVERMEAD ouest
								"1307", // !=
								"1403", // PINK/du CONSERVATOIRE sud =>
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
		map2.put(652L, new RouteTripSpec(652L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERMEAD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"9036", // FICTIF GRANDE RIVIERE-Départ
								"1056", // ++
								"1171", // Parc-O-Bus RIVERMEAD arrivée RIVERMEAD
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1377", // PARC-O-BUS RIVERMEAD
								"1054", // ++
								"9037", // FICTIF GRANDE RIVIÈRE- Arrivée
						})) //
				.compileBothTripSort());
		map2.put(653L, new RouteTripSpec(653L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERMEAD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"9036", // <> FICTIF GRANDE RIVIERE-Départ
								"1128", // != BROAD/LOUIS-SAINT-LAURENT est
								"1188", // ++
								"1358", // chemin d'AYLMER/RIVERMEAD sud
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1377", // PARC-O-BUS RIVERMEAD ouest
								"1268", // ++
								"1075", // == !=
								"9036", // != <> FICTIF GRANDE RIVIERE
								"9037", // != FICTIF GRANDE RIVIÈRE- Arrivée
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
								"2286", // SAINT-RAYMOND/ISABELLE
								"3446", // ++
								"4351", // de CANNES/de CAVAILLON ouest
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"4352", // de CANNES/de CAVAILLON est
								"4319", // ++
								"2288", // SAINT-RAYMOND/CORBEIL
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
						})) //
				.compileBothTripSort());
		map2.put(671L, new RouteTripSpec(671L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LABROSSE_STATION, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2422", // LIONEL-ÉMOND/SAINT-RAYMOND est
								"2286", // SAINT-RAYMOND/ISABELLE
								"3666", // ++
								"3990", // Terminus Labrosse
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3991", // Quai local LABROSSE #5
								"8061", // == Station de la GAPPE #1
								"8051", // != Station LAC LEAMY
								"2114", // != SAINT-JOSEPH/GAMELIN
								"2404", // == SAINT-RAYMOND/ROY
								"2288", // SAINT-RAYMOND/CORBEIL
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
		map2.put(697L, new RouteTripSpec(697L, //
				0, MTrip.HEADSIGN_TYPE_STRING, MASSON_ANGERS, //
				1, MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"2422", // LIONEL-ÉMOND/SAINT-RAYMOND
								"2286", // SAINT-RAYMOND/ISABELLE
								"4727", // ++
								"4773", // de l'ARÉNA/LOMBARD arrivée
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"4772", // de l'ARÉNA/LOMBARD
								"4706", // ++
								"2288", // SAINT-RAYMOND/CORBEIL
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
						"2210", // CENTRE ASTICOU
								"2013", // CEGEP GABRIELLE-ROY #3
								"2188", // de la CITÉ-DES-JEUNES/TALBOT
								"2272", // RIEL/ISABELLE
								"2672", // SACRÉ-COEUR/SAINT-RÉDEMPTEUR
								"2680", // LAURIER/des ALLUMETTIÈRES
						})) //
				.compileBothTripSort());
		map2.put(732L, new RouteTripSpec(732L, //
				0, MTrip.HEADSIGN_TYPE_STRING, ASTICOU_CENTER, //
				1, MTrip.HEADSIGN_TYPE_STRING, "Isabelle" + _SLASH_ + "Richard") //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"2140", // SAINT-JOSEPH/MEUNIER
								"2364", // !=
								"2358", // DANIEL-JOHNSON/LUCIEN-BRAULT
								"2342", // !=
								"2089", // CENTRE ASTICOU
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"2210", // CENTRE ASTICOU
								"2340", // !=
								"2360", // <> DANIEL-JOHNSON/RADISSON
								"2346", // <>
								"2366", // !=
								"2382", // ISABELLE/RICHARD
						})) //
				.compileBothTripSort());
		map2.put(733L, new RouteTripSpec(733L, //
				0, MTrip.HEADSIGN_TYPE_STRING, ASTICOU_CENTER, // PLATEAU, //
				1, MTrip.HEADSIGN_TYPE_STRING, FREEMAN) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"2153", // TERMINUS FREEMAN
								"2175", // des HAUTES-PLAINES/du TERROIR
								"2011", // CEGEP GABRIELLE-ROY
								"2206", // ==
								"2210", // != CENTRE ASTICOU =>
						// "2214", // != #PLATEAU
						// "2642", // != SAINT-RÉDEMPTEUR/SACRÉ-COEUR #PLATEAU => #PLATEAU
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						// "2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR #PLATEAU <= #PLATEAU
						// "2216", // != #PLATEAU
								"2210", // != CENTRE ASTICOU <=
								"2212", // ==
								"2183", // ++
								"2151", // TERMINUS Parc-o-bus FREEMAN
						})) //
				.compileBothTripSort());
		map2.put(734L, new RouteTripSpec(734L, //
				0, MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT, //
				1, MTrip.HEADSIGN_TYPE_STRING, TERRASSES_DE_LA_CHAUDIERE) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"2621", // LAURIER/ ÉLISABETH-BRUYÈRE
								"2741", // SAINT-RAYMOND/LOUISE-CAMPAGNA
								"2420", // LIONEL-ÉMOND/SAINT-RAYMOND
								"2642", // SAINT-RÉDEMPTEUR/SACRÉ-COEUR
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR
								"2291", // ++
								"2604", // TERRASSES de la CHAUDIÈRE
						})) //
				.compileBothTripSort());
		map2.put(735L, new RouteTripSpec(735L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Galène" + _SLASH_ + "Mineurs", // PM
				1, MTrip.HEADSIGN_TYPE_STRING, COLLEGE_NOUVELLES_FRONTIERES_SHORT) // "Émond / Gamelin" // AM
				.addTripSort(0, //
						Arrays.asList(new String[] { // PM
						"2210", // != CENTRE ASTICOU <=
								"2212", // <>
								"2208", // !=
								"2188", // de la CITÉ-DES-JEUNES/TALBOT #ECOLE_SECONDAIRE_MONT_BLEU
								"2653", // de la GALÈNE/des MINEURS
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { // AM
						"2777", // MARIE-BURGER/de la GALÈNE est
								"2192", // de la CITÉ-DES-JEUNES/TALBOT ouest #ECOLE_SECONDAIRE_MONT_BLEU
								"2011", // CEGEP GABRIELLE-ROY
								"2206", // ==
								"2089", // != CENTRE ASTICOU =>
								"2212", // <>
								"2312", // !=
								"2420", // != LIONEL-ÉMOND/SAINT-RAYMOND
								"2427", // LIONEL-ÉMOND / GAMELIN
						})) //
				.compileBothTripSort());
		map2.put(737L, new RouteTripSpec(737L, //
				0, MTrip.HEADSIGN_TYPE_STRING, ASTICOU_CENTER, //
				1, MTrip.HEADSIGN_TYPE_STRING, TERRASSES_DE_LA_CHAUDIERE) // PLATEAU) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						// "2642", // SAINT-RÉDEMPTEUR/SACRÉ-COEUR #PLATEAU <=
								"2602", // TERRASSES de la CHAUDIÈRE
								"2066", // SAINT-JOSEPH/ CHÂTELAIN
								"2120", // SAINT-JOSEPH/RENÉ-MARENGÈRE
								"2316", // ==
								"2190", // !=
								"2188", // != de la CITÉ-DES-JEUNES/TALBOT =>
								"2011", // <> CEGEP GABRIELLE-ROY
								"2200", // !=
								"2210", // CENTRE ASTICOU =>
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"2210", // != CENTRE ASTICOU <=
								"2221", // !=
								"2011", // != CEGEP GABRIELLE-ROY <=
								"2318", // ==
								"2360", // DANIEL-JOHNSON/RADISSON
								"2122", // SAINT-JOSEPH/RENÉ-MARENGÈRE
								"2604", // TERRASSES de la CHAUDIÈRE sud
						// "2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR #PLATEAU =>
						})) //
				.compileBothTripSort());
		map2.put(739L, new RouteTripSpec(739L, //
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
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JARDINS_LAVIGNE_SHORT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1108", // FRONT/des ALLUMETTIÈRES est
								"2604", // TERRASSES de la CHAUDIÈRE sud
								"2615", // LAVAL/LAURIER
								"2690", // des ALLUMETTIÈRES/CHAMPLAIN
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2610", // du PORTAGE/AUBRY
								"1282", // ++
								"1109", // FRONT/des ALLUMETTIÈRES ouest
						})) //
				.compileBothTripSort());
		map2.put(749L, new RouteTripSpec(749L, //
				0, MTrip.HEADSIGN_TYPE_STRING, PLATEAU, // ASTICOU_CENTER
				1, MTrip.HEADSIGN_TYPE_STRING, RIVERMEAD) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1459", // du CONSERVATOIRE/du LOUVRE
								"2273", // ==
								"2215", // !=
								"2210", // != CENTRE ASTICOU =>
								"2285", // !=
								"2642", // != SAINT-RÉDEMPTEUR/SACRÉ-COEUR #PLATEAU =>
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"2644", // != SAINT-RÉDEMPTEUR/SACRÉ-CŒUR #PLATEAU <=
								"2287", // !=
								"2089", // != CENTRE ASTICOU <=
								"2218", // !=
								"2766", // ==
								"2806", // du PLATEAU/du MARIGOT
						})) //
				.compileBothTripSort());
		map2.put(750L, new RouteTripSpec(750L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JARDINS_LAVIGNE_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"9036", // FICTIF GRANDE RIVIERE-Départ
								"1280", // ++
								"1355", // == !=
								"1377", // != <>
								"1279", // !=
								"1307", // == !=
								"2237", // du PLATEAU/des CÈDRES
								"2273", // du PLATEAU/ SAINT-RAYMOND
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2271", // du PLATEAU/ SAINT-RAYMOND
								"1306", // !=
								"1377", // <>
								"1352", // !=
								"1354", // ++
								"9037", // FICTIF GRANDE RIVIÈRE- Arrivée
						})) //
				.compileBothTripSort());
		map2.put(751L, new RouteTripSpec(751L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERMEAD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1124", // != BROAD/ANNA <=
								"9036", // != <> FICTIF GRANDE RIVIERE <=
								"1128", // == !=
								"1358", // chemin d'AYLMER/RIVERMEAD sud
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1290", // chemin d'AYLMER/GRIMES nord
								"1078", // == != WILFRID-LAVIGNE/JOHN-EGAN est
								"9036", // != <> FICTIF GRANDE RIVIERE =>
								"9037", // != FICTIF GRANDE RIVIÈRE- Arrivée =>
						})) //
				.compileBothTripSort());
		map2.put(753L, new RouteTripSpec(753L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Lucerne" + _SLASH_ + "R.Steward", // "Alymer" + _SLASH_ + "Rivermead", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"9036", // <> != FICTIF GRANDE RIVIERE-Départ <=
								"1128", // != == BROAD/LOUIS-SAINT-LAURENT <=
								"1073", // ++
								"1263", // CÔTÉ/LUCERNE
								"1358", // chemin d'AYLMER/RIVERMEAD
								"2298", // de LUCERNE/ROBERT/STEWARD
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1299", // de LUCERNE/RIVERMEAD
								"1298", // de LUCERNE/ROBERT-STEWARD
								"1268", // ++
								"1075", // ++
								"1082", // == !=
								"9037", // != FICTIF GRANDE RIVIÈRE- Arrivée =>
						})) //
				.compileBothTripSort());
		map2.put(754L, new RouteTripSpec(754L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "McConnell" + _SLASH_ + "Vanier", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_GRANDE_RIVIERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"9036", // FICTIF GRANDE RIVIERE-Départ <=
								"1128", // ++ BROAD/LOUIS-SAINT-LAURENT
								"1347", // MC CONNELL/MORLEY-WALTERS nord
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1333", // MC CONNELL/MORLEY-WALTERS sud
								"1400", // ==
								"1377", // !=
								"1171", // !=
								"1352", // ==
								"1075", // ++ WILFRID-LAVIGNE/LEGUERRIER
								"1078", // ++ WILFRID-LAVIGNE/JOHN-EGAN est
								"9037", // FICTIF GRANDE RIVIÈRE- Arrivée =>
						})) //
				.compileBothTripSort());
		map2.put(767L, new RouteTripSpec(767L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, P_O_B_LES_PROMENDADES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR est #E_S_DE_L_ILE
								"3066", // ++ GRÉBER/de la SAVANE est
								"3000", // LES PROMENADES
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2714", // FOURNIER/JOANISSE ouest
								"2696", // ++
								"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR est #E_S_DE_L_ILE
								"2624", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR #E_S_DE_L_ILE
						})) //
				.compileBothTripSort());
		map2.put(811L, new RouteTripSpec(811L, //
				0, MTrip.HEADSIGN_TYPE_STRING, GALERIES_AYLMER_SHORT, //
				1, MTrip.HEADSIGN_TYPE_STRING, PARC_LA_BAIE) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"3217", // SAINT-LOUIS/SAINT-ANTOINE
								"2586", // ++
								"1077", // GALERIES AYLMER arrivée
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1079", // GALERIES AYLMER départ
								"2408", // ++
								"2726", // FOURNIER/du LAC LEAMY
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
		map2.put(825L, new RouteTripSpec(825L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PLATEAU) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1459", // du CONSERVATOIRE/du LOUVRE
								"2213", // ++
								"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR #E_S_DE_L_ILE
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2642", // SAINT-RÉDEMPTEUR/SACRÉ-COEUR #E_S_DE_L_ILE
								"2289", // ++
								"1460", // du CONSERVATOIRE/du LOUVRE
						})) //
				.compileBothTripSort());
		map2.put(829L, new RouteTripSpec(829L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Le Manoir") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2775", // LOUISE-CAMPAGNA/SAINT-RAYMOND
								"2736", // ++
								"2006", // ALEXANDRE-TACHÉ/SAINT-DOMINIQUE
								"2070", // ++
								"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR #E_S_DE_L_ILE
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2642", // SAINT-RÉDEMPTEUR/SACRÉ-COEUR #E_S_DE_L_ILE
								"2289", // ++
								"2006", // ALEXANDRE-TACHÉ/SAINT-DOMINIQUE
						})) //
				.compileBothTripSort());
		map2.put(831L, new RouteTripSpec(831L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CEGEP_GABRIELLE_ROY_SHORT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2013", // CEGEP GABRIELLE-ROY #3
								"2272", // RIEL/ISABELLE
								"2672", // SACRÉ-COEUR/SAINT-RÉDEMPTEUR #E_S_DE_L_ILE
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2650", // SACRÉ-COEUR/SAINT-HENRI #E_S_DE_L_ILE
								"2276", // ++
								"2008", // CEGEP GABRIELLE-ROY/arrivée
						})) //
				.compileBothTripSort());
		map2.put(834L, new RouteTripSpec(834L, //
				0, MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT, // PLATEAU, // ASTICOU_CENTER
				1, MTrip.HEADSIGN_TYPE_STRING, TERRASSES_DE_LA_CHAUDIERE) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"2741", // SAINT-RAYMOND/LOUISE-CAMPAGNA
								"2239", // du PLATEAU/des CÈDRES
								"2769", // ==
								"2285", // !=
								"2642", // != SAINT-RÉDEMPTEUR/SACRÉ-COEUR #PLATEAU =>
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"2644", // != SAINT-RÉDEMPTEUR/SACRÉ-CŒUR #PLATEAU <=
								"2287", // !=
								"2767", // ==
								"2006", // ALEXANDRE-TACHÉ/SAINT-DOMINIQUE
								"2064", // ALEXANDRE-TACHÉ/SAINT-JOSEPH
								"2604", // TERRASSES de la CHAUDIÈRE
						})) //
				.compileBothTripSort());
		map2.put(837L, new RouteTripSpec(837L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "St-Joseph") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2006", // ALEXANDRE-TACHÉ/SAINT-DOMINIQUE
								"2066", // RIEL/ISABELLE
								"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR #E_S_DE_L_ILE
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2642", // SAINT-RÉDEMPTEUR/SACRÉ-COEUR #E_S_DE_L_ILE
								"2068", // ++
								"2004", // ALEXANDRE-TACHÉ/SAINT-DOMINIQUE
						})) //
				.compileBothTripSort());
		map2.put(839L, new RouteTripSpec(839L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_MONT_BLEU, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Taché" + _SLASH_ + "St-Joseph") //
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
		map2.put(867L, new RouteTripSpec(867L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LES_PROMENADES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ECOLE_SECONDAIRE_DE_L_ILE_SHORT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2644", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR
								"3000", // LES PROMENADES
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2714", // FOURNIER/JOANISSE
								"2624", // SAINT-RÉDEMPTEUR/SACRÉ-CŒUR
						})) //
				.compileBothTripSort());
		map2.put(870L, new RouteTripSpec(870L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Arena Guertin", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAURIER + _SLASH_ + ALLUMETTIERES_SHORT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2682", // LAURIER/des ALLUMETTIÈRES
								"2643", // SAINT-RÉDEMPTEUR/ALLARD
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2643", // SAINT-RÉDEMPTEUR/ALLARD
								"2682", // LAURIER/des ALLUMETTIÈRES
						})) //
				.compileBothTripSort());
		map2.put(931L, new RouteTripSpec(931L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAURIER + _SLASH_ + ALLUMETTIERES_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ASTICOU_CENTER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2210", // CENTRE ASTICOU
								"2274", // ++
								"2680", // LAURIER/des ALLUMETTIÈRES
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2424", // LIONEL-ÉMOND/ GAMELIN
								"2282", // ISABELLE/FRÉCHETTE
								"2242", // ++
								"2089", // CENTRE ASTICOU
						})) //
				.compileBothTripSort());
		map2.put(932L, new RouteTripSpec(932L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Isabelle" + _SLASH_ + "Richard", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ASTICOU_CENTER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2210", // CENTRE ASTICOU
								"2266", // ++
								"2382", // ISABELLE/RICHARD
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2140", // SAINT-JOSEPH/MEUNIER
								"2364/", // ++
								"2358", // DANIEL-JOHNSON/LUCIEN-BRAULT
								"2274", // ++
								"2089", // CENTRE ASTICOU
						})) //
				.compileBothTripSort());
		map2.put(933L, new RouteTripSpec(933L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FREEMAN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ASTICOU_CENTER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2210", // CENTRE ASTICOU
								"2183", // ++
								"2151", // TERMINUS Parc-o-bus FREEMAN
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2153", // TERMINUS FREEMAN
								"2169/", // ++
								"2175", // des HAUTES-PLAINES/du TERROIR
								"2187", // ++
								"2210", // CENTRE ASTICOU
						})) //
				.compileBothTripSort());
		map2.put(934L, new RouteTripSpec(934L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ASTICOU_CENTER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Taché" + _SLASH_ + "St-Joseph") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2004", // ALEXANDRE-TACHÉ/SAINT-DOMINIQUE
								"2774", // ++
								"2239", // du PLATEAU/des CÈDRES
								"2788", // ++
								"2210", // CENTRE ASTICOU
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2089", // CENTRE ASTICOU
								"2213", // ++
								"2006", // ALEXANDRE-TACHÉ/SAINT-DOMINIQUE
								"2026", // ++
								"2064", // ALEXANDRE-TACHÉ/SAINT-JOSEPH
						})) //
				.compileBothTripSort());
		map2.put(935L, new RouteTripSpec(935L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ASTICOU_CENTER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Galène" + _SLASH_ + "Mineurs") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2777", // MARIE-BURGER/de la GALÈNE
								"2197", // ++
								"2089", // CENTRE ASTICOU
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2210", // CENTRE ASTICOU
								"2191", // ++
								"2653", // de la GALÈNE/des MINEURS
						})) //
				.compileBothTripSort());
		map2.put(937L, new RouteTripSpec(937L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, TERRASSES_DE_LA_CHAUDIERE, // "Taché" + SLASH + "Front", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ASTICOU_CENTER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2210", // CENTRE ASTICOU
								"2141", // ++
								"2604", // TERRASSES de la CHAUDIÈRE
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2548", // ALEXANDRE-TACHÉ/FRONT
								"2380/", // ++
								"2210", // CENTRE ASTICOU
						})) //
				.compileBothTripSort());
		map2.put(949L, new RouteTripSpec(949L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERMEAD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ASTICOU_CENTER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2210", // CENTRE ASTICOU
								"1466", // ++
								"1171", // Parc-O-Bus RIVERMEAD arrivée RIVERMEAD
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1377", // PARC-O-BUS RIVERMEAD
								"1394/", // ++
								"1051", // VANIER/du PLATEAU
								"2782/", // ++
								"2089", // CENTRE ASTICOU
						})) //
				.compileBothTripSort());
		map2.put(950L, new RouteTripSpec(950L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, GALERIES_AYLMER_SHORT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FRONT + _SLASH_ + "Harvey") //
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
		if (mRoute.getId() == 990L) {
			if (gTrip.getTripHeadsign().equalsIgnoreCase("TEST ROUTIER")) {
				mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()) + " " + gTrip.getDirectionId(), gTrip.getDirectionId());
				return;
			}
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getHeadsignValue().equalsIgnoreCase("EN TRANSIT")) {
			mTrip.setHeadsignString(mTripToMerge.getHeadsignValue(), mTrip.getHeadsignId());
			return true;
		} else if (mTripToMerge.getHeadsignValue().equalsIgnoreCase("EN TRANSIT")) {
			mTripToMerge.setHeadsignString(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignId());
			return true;
		}
		if (mTrip.getHeadsignValue().equalsIgnoreCase("DÉSOLÉ,F-D SERV")) {
			mTrip.setHeadsignString(mTripToMerge.getHeadsignValue(), mTrip.getHeadsignId());
			return true;
		} else if (mTripToMerge.getHeadsignValue().equalsIgnoreCase("DÉSOLÉ,F-D SERV")) {
			mTripToMerge.setHeadsignString(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignId());
			return true;
		}
		if (mTrip.getRouteId() == 11L) {
			if (Arrays.asList( //
					PLACE_D_ACCUEIL, //
					OTTAWA // ++
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 17L) {
			if (Arrays.asList( //
					PLACE_D_ACCUEIL, //
					OTTAWA // ++
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 20L) {
			if (Arrays.asList( //
					TERRASSES, //
					"Mhistoire", //
					OTTAWA + _SLASH_ + "Portage", //
					OTTAWA // ++
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 21L) {
			if (Arrays.asList( //
					"Casino", //
					FREEMAN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Casino", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Casino", //
					OTTAWA //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 23L) {
			if (Arrays.asList( //
					"Conservatoire", //
					PLATEAU //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PLATEAU, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 27L) {
			if (Arrays.asList( //
					"Galène", //
					"Hplaines" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Hplaines", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 26L) {
			if (Arrays.asList( //
					OTTAWA, // ==
					PLATEAU //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PLATEAU, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 28L) {
			if (Arrays.asList( //
					PLATEAU, //
					GAMELIN_EMOND //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(GAMELIN_EMOND, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 31L) {
			if (Arrays.asList( //
					PLACE_D_ACCUEIL, //
					CEGEP_GABRIELLE_ROY_SHORT //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY_SHORT, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					CEGEP_GABRIELLE_ROY_SHORT, //
					OTTAWA //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					CEGEP_GABRIELLE_ROY_SHORT, //
					OTTAWA, //
					LAURIER //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LAURIER, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 32L) {
			if (Arrays.asList( //
					CEGEP_GABRIELLE_ROY_SHORT, //
					"Ed.Lst-Lau" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY_SHORT, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					LAURIER, //
					OTTAWA //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 33L) {
			if (Arrays.asList( // #GATINEAU
					FREEMAN, //
					CEGEP_GABRIELLE_ROY_SHORT + _SLASH_ + FREEMAN, //
					CEGEP_GABRIELLE_ROY_SHORT + _SLASH_ + FREEMAN + _SLASH_ + DE_LA_CITÉ, //
					DE_LA_CITÉ //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DE_LA_CITÉ, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( // #OTTAWA
					TERRASSES, //
					"Mhistoire", //
					CEGEP_GABRIELLE_ROY_SHORT, //
					FREEMAN + _SLASH_ + CEGEP_GABRIELLE_ROY_SHORT + _SLASH_ + OTTAWA, //
					OTTAWA //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 34L) {
			if (Arrays.asList( //
					P_O_B_ALLUMETTIERES, //
					PLATEAU //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PLATEAU, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 35L) {
			if (Arrays.asList( //
					"M.Burger", //
					"H.De-Ville", //
					CEGEP_GABRIELLE_ROY_SHORT //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY_SHORT, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 36L) {
			if (Arrays.asList( //
					OTTAWA, // ==
					CEGEP_GABRIELLE_ROY_SHORT //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY_SHORT, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 37L) {
			if (Arrays.asList( //
					"H.De-Ville", //
					LAURIER, //
					CEGEP_GABRIELLE_ROY_SHORT //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY_SHORT, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Mhistoire", //
					OTTAWA //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 38L) {
			if (Arrays.asList( //
					"H.De-Ville", //
					FREEMAN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(FREEMAN, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 39L) {
			if (Arrays.asList( //
					PLATEAU, //
					OTTAWA //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(OTTAWA, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 40L) {
			if (Arrays.asList( //
					FRONT + _SLASH_ + ALLUMETTIERES_SHORT, //
					JARDINS_LAVIGNE_SHORT //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(JARDINS_LAVIGNE_SHORT, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 48L) {
			if (Arrays.asList( //
					"Lucerne", //
					"Cook" + _SLASH_ + "Perry" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Cook" + _SLASH_ + "Perry", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 49L) {
			if (Arrays.asList( //
					CEGEP_GABRIELLE_ROY_SHORT, //
					GAMELIN_EMOND //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY_SHORT, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					GALERIES_AYLMER_SHORT, //
					GAMELIN_EMOND //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(GALERIES_AYLMER_SHORT, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 50L) {
			if (Arrays.asList( //
					RIVERMEAD, //
					GALERIES_AYLMER_SHORT, //
					P_O_B_ALLUMETTIERES //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(P_O_B_ALLUMETTIERES, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 52L) {
			if (Arrays.asList( //
					P_O_B_ALLUMETTIERES, //
					LAVIGNE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LAVIGNE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 62L) {
			if (Arrays.asList( //
					"Nobert", //
					"Davidson" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Davidson", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 64L) {
			if (Arrays.asList( //
					ARENA_BEAUDRY, //
					CEGEP_GABRIELLE_ROY_SHORT, //
					FREEMAN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(FREEMAN, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					COLLEGE_NOUVELLES_FRONTIERES_SHORT, //
					CEGEP_GABRIELLE_ROY_SHORT //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CEGEP_GABRIELLE_ROY_SHORT, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 66L) {
			if (Arrays.asList( //
					PROMENADES, //
					ARENA_BEAUDRY //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(ARENA_BEAUDRY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 68L) {
			if (Arrays.asList( //
					"E Montbleu", //
					LABROSSE, //
					"Affaires" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Affaires", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					LABROSSE, //
					"Affaires", //
					"Affaires" + _SLASH_ + "Entreprises" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Affaires" + _SLASH_ + "Entreprises", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 78L) {
			if (Arrays.asList( //
					P_O_B_LORRAIN, //
					"Cheval-Blanc", //
					"Chev Blanc" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Chev Blanc", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 87L) {
			if (Arrays.asList( //
					PLACE_D_ACCUEIL, //
					OTTAWA //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(PLACE_D_ACCUEIL, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 88L) {
			if (Arrays.asList( //
					LORRAIN, //
					LABROSSE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(LABROSSE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 338L) {
			if (Arrays.asList( //
					COLLEGE_NOUVELLES_FRONTIERES_SHORT, //
					COLLEGE_SAINT_ALEXANDRE_SHORT //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(COLLEGE_SAINT_ALEXANDRE_SHORT, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 649L) {
			if (Arrays.asList( //
					"ES De L'Île", //
					"E Montbleu" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("E Montbleu", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 731L) {
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
		} else if (mTrip.getRouteId() == 733L) {
			if (Arrays.asList( //
					"ES De L'Île", //
					"E Montbleu" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("E Montbleu", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 800L) {
			if (Arrays.asList( //
					GALERIES_AYLMER_SHORT, //
					P_O_B_ALLUMETTIERES //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(P_O_B_ALLUMETTIERES, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 850L) {
			if (Arrays.asList( //
					COLLEGE_NOUVELLES_FRONTIERES_SHORT, //
					P_O_B_ALLUMETTIERES //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(COLLEGE_NOUVELLES_FRONTIERES_SHORT, mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnexpected trips to merge: %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern MUSEE_CANADIEN_HISTOIRE_ = Pattern.compile("((^|\\W)(mus[e|é]e canadien de l'histoire)(\\W|$))",
			Pattern.CASE_INSENSITIVE);
	private static final String MUSEE_CANADIEN_HISTOIRE_REPLACEMENT = "$2" + MUSEE_CANADIEN_HISTOIRE_SHORT + "$4";

	private static final Pattern CLEAN_STATION = Pattern.compile("((^|\\W)(station|ston|sta)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_STATION_REPLACEMENT = "$2" + STATION_ + "$4";

	private static final Pattern CEGEP_GABRIELLE_ROY_ = Pattern.compile("((^|\\W)(" //
			+ "c[é|É|e|è|È]gep gabrielle-roy" //
			+ "|" //
			+ "cegep gab\\.roy" //
			+ "|" //
			+ "cegep gab-roy"//
			+ "|" //
			+ "c[é|É|e|è|È]gep groy" //
			+ "|" //
			+ "cegep g-roy" //
			+ "|" //
			+ "c[é|e]gep g\\.roy" //
			+ "|" //
			+ "cgp gabrielle-r" //
			+ "|" //
			+ "cgp groy" //
			+ "|" //
			+ "cgp g-roy" //
			+ "|" //
			+ "g-roy" //
			+ ")(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String CEGEP_GABRIELLE_ROY_REPLACEMENT = "$2" + CEGEP_GABRIELLE_ROY_SHORT + "$4";

	private static final Pattern ECOLE_SECONDAIRE_DE_L_ILE_ = Pattern.compile("((^|\\W)([e|é]cole De l'[i|î]le)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ECOLE_SECONDAIRE_DE_L_ILE_REPLACEMENT = "$2" + ECOLE_SECONDAIRE_DE_L_ILE_SHORT + "$4";

	private static final Pattern JARDINS_LAVIGNE_ = Pattern.compile("((^|\\W)(jardins lavigne|jlavigne)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String JARDINS_LAVIGNE_REPLACEMENT = "$2" + JARDINS_LAVIGNE_SHORT + "$4";

	private static final Pattern GALERIES_AYLMER_ = Pattern.compile("((^|\\W)(galeries aylmer|gal\\.aylmer)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String GALERIES_AYLMER_REPLACEMENT = "$2" + GALERIES_AYLMER_SHORT + "$4";

	private static final Pattern COLLEGE_ = Pattern.compile("((^|\\W)(college)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String COLLEGE_REPLACEMENT = "$2" + COLLEGE_SHORT + "$4";

	private static final Pattern COLLEGE_NOUVELLES_FRONTIERES_ = Pattern.compile("((^|\\W)(col nf|coll[é|e]ge nf)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String COLLEGE_NOUVELLES_FRONTIERES_REPLACEMENT = "$2" + COLLEGE_NOUVELLES_FRONTIERES_SHORT + "$4";

	private static final Pattern COLLEGE_SAINT_JOSEPH_ = Pattern.compile("((^|\\W)(c stjoseph)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String COLLEGE_SAINT_JOSEPH_REPLACEMENT = "$2" + COLLEGE_SAINT_JOSEPH_SHORT + "$4";

	private static final Pattern COLLEGE_SAINY_ALEXANDRE_ = Pattern.compile("((^|\\W)(col stalex)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String COLLEGE_SAINY_ALEXANDRE_REPLACEMENT = "$2" + COLLEGE_SAINT_ALEXANDRE_SHORT + "$4";

	private static final Pattern ALLUMETTIERES_ = Pattern.compile("((^|\\W)(des allumetti[è|e]res|allumetti[è|e]res|allum)(\\W|$))",
			Pattern.CASE_INSENSITIVE);
	private static final String ALLUMETTIERES_REPLACEMENT = "$2" + ALLUMETTIERES_SHORT + "$4";

	private static final Pattern COTES_DES_NEIGES_ = Pattern.compile("((^|\\W)(c[ô|o]tes-des-neiges|coteneige)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String COTES_DES_NEIGES_REPLACEMENT = "$2" + COTES_DES_NEIGES + "$4";

	private static final Pattern P_O_B = Pattern.compile("((^|\\W)(pob|p-o-b|parc o bus|parc-o-bus)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String P_O_B_REPLACEMENT = "$2" + P_O_B_SHORT + "$4";

	private static final Pattern PRE_TUNNEY_ = Pattern.compile("((^|\\W)(pr[e|é|É] tunney)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String PRE_TUNNEY__REPLACEMENT = "$2" + "Pré-Tunney" + "$4";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = CLEAN_STATION.matcher(tripHeadsign).replaceAll(CLEAN_STATION_REPLACEMENT);
		tripHeadsign = CEGEP_GABRIELLE_ROY_.matcher(tripHeadsign).replaceAll(CEGEP_GABRIELLE_ROY_REPLACEMENT);
		tripHeadsign = COLLEGE_.matcher(tripHeadsign).replaceAll(COLLEGE_REPLACEMENT);
		tripHeadsign = COLLEGE_NOUVELLES_FRONTIERES_.matcher(tripHeadsign).replaceAll(COLLEGE_NOUVELLES_FRONTIERES_REPLACEMENT);
		tripHeadsign = COLLEGE_SAINT_JOSEPH_.matcher(tripHeadsign).replaceAll(COLLEGE_SAINT_JOSEPH_REPLACEMENT);
		tripHeadsign = COLLEGE_SAINY_ALEXANDRE_.matcher(tripHeadsign).replaceAll(COLLEGE_SAINY_ALEXANDRE_REPLACEMENT);
		tripHeadsign = ECOLE_SECONDAIRE_DE_L_ILE_.matcher(tripHeadsign).replaceAll(ECOLE_SECONDAIRE_DE_L_ILE_REPLACEMENT);
		tripHeadsign = GALERIES_AYLMER_.matcher(tripHeadsign).replaceAll(GALERIES_AYLMER_REPLACEMENT);
		tripHeadsign = JARDINS_LAVIGNE_.matcher(tripHeadsign).replaceAll(JARDINS_LAVIGNE_REPLACEMENT);
		tripHeadsign = ALLUMETTIERES_.matcher(tripHeadsign).replaceAll(ALLUMETTIERES_REPLACEMENT);
		tripHeadsign = COTES_DES_NEIGES_.matcher(tripHeadsign).replaceAll(COTES_DES_NEIGES_REPLACEMENT);
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
					return 100_000 + digits;
				}
			}
			System.out.printf("\nUnexpected stop ID for %s!\n", gStop);
			System.exit(-1);
			return -1;
		}
		return super.getStopId(gStop);
	}
}