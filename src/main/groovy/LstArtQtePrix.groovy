/**
 * README
 * This extension is used by Mashup
 *
 * Name : EXT030MI.LstArtQtePrix
 * Description : The LstArtQtePrix list item qty price
 * Date         Changed By   Description
 * 20210831     APACE        COMX01 - Management of customer agreement
 * 20220128     APACE        logger.info and end of line semicolon have been removed
 * 20220217     MBEN         Description of input/output parameters modified
 * 20221110     ARENARD      Input parameters for OIS060MI.GetCustBlkAgrLn have been modified
 * 20230420     MLECLERCQ     Removed Forced AGPR and DIPR if no record in OAGRPR
 * 20230801     MLECLERCQ     Get last price even if LVDT is outdated
 */

public class LstArtQtePrix extends ExtendM3Transaction {

  private final MIAPI mi
  private final DatabaseAPI database
  private final LoggerAPI logger
  private final MICallerAPI miCaller
  private final ProgramAPI program
  private final UtilityAPI utility
  private DBContainer OAGRPRSave
  int currentCompany = 0
  String cuno = ""
  String agno = ""
  String prrf = ""
  int stdt = 0
  int fvdt = 0

  public LstArtQtePrix(MIAPI mi, DatabaseAPI database, LoggerAPI logger, ProgramAPI program,UtilityAPI utility, MICallerAPI miCaller) {
    this.mi = mi
    this.database = database
    this.logger = logger
    this.program = program
    this.utility = utility
    this.miCaller = miCaller
  }
  //Read items qty price for mashup H5
  public void main() {
    currentCompany = (Integer)program.getLDAZD().CONO

    if(mi.in.get("CUNO") != null){
      cuno = mi.in.get("CUNO")
    }
    if(mi.in.get("AGNO") != null){
      agno = mi.in.get("AGNO")
    }
    if(mi.in.get("STDT") != null){
      stdt = mi.in.get("STDT")
    }
    if(mi.in.get("FVDT") != null){
      fvdt = mi.in.get("FVDT")
    }
    if(mi.in.get("PRRF") != null){
      prrf = mi.in.get("PRRF")
    }

    DBAction action = database.table("OAGRLN").index("00").selection("UWCONO", "UWCUNO", "UWAGNO", "UWFDAT","UWUNIT","UWCUCD","UWLIDT","UWAGQT","UWSTDT","UWLVDT","UWAGST","UWAGLN").build()
    DBContainer OAGRLN = action.createContainer()
    OAGRLN.set("UWCONO", currentCompany)
    OAGRLN.setString("UWCUNO", cuno)
    OAGRLN.setString("UWAGNO", agno)
    OAGRLN.set("UWFDAT", stdt as Integer)

    action.readAll(OAGRLN, 4, releaseExtends)
  }
  //Read OAGRLN lines
  Closure<?> releaseExtends = { DBContainer OAGRLN ->
    boolean findOAGRP = false
    DBAction RechercheOAGRPR = database.table("OAGRPR").index("00").selection("OLSACD","OLDIPC","OLQTYL","OLAGPR","OLSTDT","OLFDAT","OLPREX","OLOBV1").build()
    DBContainer OAGRPR = RechercheOAGRPR.getContainer()
    OAGRPR.set("OLCONO", currentCompany)
    OAGRPR.setInt("OLSTDT", OAGRLN.get("UWSTDT") as Integer)
    OAGRPR.setInt("OLFDAT", stdt as Integer)
    OAGRPR.setString("OLCUNO", cuno)
    OAGRPR.setString("OLAGNO", agno)
    OAGRPR.setString("OLPREX", OAGRLN.get("UWPREX").toString())
    OAGRPR.setString("OLOBV1", OAGRLN.get("UWOBV1").toString())
    //Read OAGRPR lines
    RechercheOAGRPR.readAll(OAGRPR, 7, {DBContainer OAGRPRresult ->
      mi.outData.put("AGNO", OAGRLN.get("UWAGNO").toString())
      mi.outData.put("OBV1", OAGRLN.get("UWOBV1").toString())
      mi.outData.put("STDT", OAGRLN.get("UWSTDT").toString())
      mi.outData.put("PREX", OAGRLN.get("UWPREX").toString())
      mi.outData.put("AGST", OAGRLN.get("UWAGST").toString())
      mi.outData.put("LVDT", OAGRLN.get("UWLVDT").toString())
      mi.outData.put("AGQT", OAGRLN.get("UWAGQT").toString())
      mi.outData.put("LIDT", OAGRLN.get("UWLIDT").toString())
      mi.outData.put("CUCD", OAGRLN.get("UWCUCD").toString())
      mi.outData.put("UNIT", OAGRLN.get("UWUNIT").toString())
      mi.outData.put("AGLN", OAGRLN.get("UWAGLN").toString())


      DBAction RechercheMITMAS = database.table("MITMAS").index("00").selection("MMSPUN","MMITDS").build()
      DBContainer MITMAS = RechercheMITMAS.getContainer()
      MITMAS.set("MMCONO", currentCompany)
      MITMAS.setString("MMITNO", OAGRLN.get("UWOBV1").toString())
      if(RechercheMITMAS.read(MITMAS)){
        mi.outData.put("ITDS", MITMAS.get("MMITDS").toString())
        mi.outData.put("SPUN", MITMAS.get("MMSPUN").toString())
      }



      DBAction RechercheEXT061 = database.table("EXT061").index("00").selection("EXZLOT","EXZSLO","EXTX40","EXZQTS","EXZUNS","EXZTRE","EXSTAC").build()
      DBContainer EXT061 = RechercheEXT061.getContainer()
      EXT061.set("EXCONO", currentCompany)
      EXT061.setInt("EXSTDT", OAGRLN.get("UWSTDT") as Integer)
      EXT061.setInt("EXFDAT", OAGRLN.get("UWFDAT") as Integer)
      EXT061.setString("EXCUNO", cuno)
      EXT061.setString("EXAGNO", agno)
      EXT061.setString("EXPREX", OAGRLN.get("UWPREX").toString())
      EXT061.setString("EXOBV1", OAGRLN.get("UWOBV1").toString())
      if(RechercheEXT061.read(EXT061)){
        mi.outData.put("ZLOT", EXT061.get("EXZLOT").toString())
        mi.outData.put("ZSLO", EXT061.get("EXZSLO").toString())
        mi.outData.put("TX40", EXT061.get("EXTX40").toString())
        mi.outData.put("ZQTS", EXT061.get("EXZQTS").toString())
        mi.outData.put("ZUNS", EXT061.get("EXZUNS").toString())
        mi.outData.put("ZTRE", EXT061.get("EXZTRE").toString())
        mi.outData.put("STAC", EXT061.get("EXSTAC").toString())
      }

      String sapr = "0.00"
      double sacdOIS017 = 0

      int tarifDate = 0;

      //Get last price even if LVDT is outdated
      DBAction RechercheOPRICH = database.table("OPRICH").index("00").reverse().selection("OJCONO","OJPRRF","OJCUCD","OJCUNO","OJFVDT").build()
      DBContainer OPRICH = RechercheOPRICH.getContainer()
      OPRICH.set("OJCONO",currentCompany)
      OPRICH.set("OJCUNO","")
      OPRICH.set("OJPRRF",prrf)
      OPRICH.set("OJCUCD",OAGRLN.get("UWCUCD").toString())
      RechercheOPRICH.readAll(OPRICH,4,{ DBContainer OPRICHresult ->

        int oagrlnStdt = OAGRLN.get("UWSTDT") as Integer
        int oprichFvdt = OPRICHresult.get("OJFVDT") as Integer
        if( oagrlnStdt >= oprichFvdt){
          if((tarifDate != 0 && tarifDate < oprichFvdt) || tarifDate == 0){
            tarifDate = OPRICHresult.get("OJFVDT") as Integer
          }
        }
      })


      logger.debug("Tarif Date: "+tarifDate)
      def params = ["PRRF": prrf, "CUCD": OAGRLN.get("UWCUCD").toString(), "FVDT": String.valueOf(tarifDate),"ITNO": OAGRLN.get("UWOBV1").toString()]
      Closure<?> closure = {Map<String, String> response ->
        logger.debug("Response = ${response}")
        if(response.error == null){
          sapr = response.SAPR
          sacdOIS017 = response.SACD as double
        }else{
          sapr = "0.00"
        }
      }

      miCaller.call("OIS017MI", "GetBasePrice", params, closure)

      if(sacdOIS017>0){
        double saprValue = (sapr as double)*sacdOIS017
        mi.outData.put("SAPR",saprValue as String)
      }else{
        mi.outData.put("SAPR",sapr as String)
      }

      double saprCalcul = sapr as Double
      double agpr = OAGRPRresult.get("OLAGPR").toString() as Double
      double discount = OAGRPRresult.get("OLDIPC").toString() as Double
      double sacdCalcul = OAGRPRresult.get("OLSACD").toString() as Double

      String calcPrice
      if (agpr > 0 && agpr != saprCalcul) {
        if(sacdCalcul>0){
          calcPrice = (agpr - (agpr * (discount / 100)))*sacdCalcul
        }else{
          calcPrice = agpr - (agpr * (discount / 100))
        }
      } else if ((agpr == 0 && saprCalcul > 0) || (agpr == saprCalcul)) {
        if(sacdOIS017>0){
          calcPrice = (saprCalcul - (saprCalcul * (discount / 100)))*sacdOIS017
        }else{
          calcPrice = saprCalcul - (saprCalcul * (discount / 100))
        }
      }
      sapr = calcPrice

      mi.outData.put("DIPR", sapr)//17122021
      String reqt=""
      String dlqt=""


      def params2 = ["CUNO":cuno, "AGNO":agno,"FDAT":OAGRLN.get("UWFDAT").toString()+"","PREX":OAGRLN.get("UWPREX").toString(),"STDT":OAGRLN.get("UWSTDT").toString()+"","OBV1":OAGRLN.get("UWOBV1").toString()]
      Closure<?> closure2 = {Map<String, String> response ->
        logger.debug("Response = ${response}")
        if(response.error == null){
          reqt = response.REQT
          dlqt = response.DLQT
        }else{
          reqt = "0.00"
          dlqt = "0.00"
        }
      }
      miCaller.call("OIS060MI", "GetCustBlkAgrLn", params2, closure2)

      mi.outData.put("REQT", reqt)
      mi.outData.put("DLQT", dlqt)

      mi.outData.put("SACD", OAGRPRresult.get("OLSACD").toString())
      mi.outData.put("DIPC", OAGRPRresult.get("OLDIPC").toString())
      mi.outData.put("QTYL", OAGRPRresult.get("OLQTYL").toString())
      mi.outData.put("AGPR", OAGRPRresult.get("OLAGPR").toString())//17122021
      Double SACD = OAGRPRresult.get("OLSACD").toString() as Double
      Double AGPR = OAGRPRresult.get("OLAGPR").toString() as Double
      Double calcul = 0
      if(SACD>0){
        calcul = AGPR*SACD
      }else{
        calcul = AGPR*1
      }
      mi.outData.put("AGPR", calcul.toString())//17122021

      mi.write()
      findOAGRP = true
    })
    if(!findOAGRP){
      mi.outData.put("AGNO", OAGRLN.get("UWAGNO").toString())
      mi.outData.put("OBV1", OAGRLN.get("UWOBV1").toString())
      mi.outData.put("STDT", OAGRLN.get("UWSTDT").toString())
      mi.outData.put("PREX", OAGRLN.get("UWPREX").toString())
      mi.outData.put("AGST", OAGRLN.get("UWAGST").toString())
      mi.outData.put("LVDT", OAGRLN.get("UWLVDT").toString())
      mi.outData.put("AGQT", OAGRLN.get("UWAGQT").toString())
      mi.outData.put("LIDT", OAGRLN.get("UWLIDT").toString())
      mi.outData.put("CUCD", OAGRLN.get("UWCUCD").toString())
      mi.outData.put("UNIT", OAGRLN.get("UWUNIT").toString())
      mi.outData.put("AGLN", OAGRLN.get("UWAGLN").toString())


      DBAction RechercheMITMAS = database.table("MITMAS").index("00").selection("MMSPUN","MMITDS").build()
      DBContainer MITMAS = RechercheMITMAS.getContainer()
      MITMAS.set("MMCONO", currentCompany)
      MITMAS.setString("MMITNO", OAGRLN.get("UWOBV1").toString())
      if(RechercheMITMAS.read(MITMAS)){
        mi.outData.put("ITDS", MITMAS.get("MMITDS").toString())
        mi.outData.put("SPUN", MITMAS.get("MMSPUN").toString())
      }



      DBAction RechercheEXT061 = database.table("EXT061").index("00").selection("EXZLOT","EXZSLO","EXTX40","EXZQTS","EXZUNS","EXZTRE","EXSTAC").build()
      DBContainer EXT061 = RechercheEXT061.getContainer()
      EXT061.set("EXCONO", currentCompany)
      EXT061.setInt("EXSTDT", OAGRLN.get("UWSTDT") as Integer)
      EXT061.setInt("EXFDAT", OAGRLN.get("UWFDAT") as Integer)
      EXT061.setString("EXCUNO", cuno)
      EXT061.setString("EXAGNO", agno)
      EXT061.setString("EXPREX", OAGRLN.get("UWPREX").toString())
      EXT061.setString("EXOBV1", OAGRLN.get("UWOBV1").toString())
      if(RechercheEXT061.read(EXT061)){
        mi.outData.put("ZLOT", EXT061.get("EXZLOT").toString())
        mi.outData.put("ZSLO", EXT061.get("EXZSLO").toString())
        mi.outData.put("TX40", EXT061.get("EXTX40").toString())
        mi.outData.put("ZQTS", EXT061.get("EXZQTS").toString())
        mi.outData.put("ZUNS", EXT061.get("EXZUNS").toString())
        mi.outData.put("ZTRE", EXT061.get("EXZTRE").toString())
        mi.outData.put("STAC", EXT061.get("EXSTAC").toString())
      }

      String sapr = "0.00"
      double sacdOIS017 = 0


      int tarifDate = 0;

      //Get last price even if LVDT is outdated
      DBAction RechercheOPRICH = database.table("OPRICH").index("00").reverse().selection("OJCONO","OJPRRF","OJCUCD","OJCUNO","OJFVDT").build()
      DBContainer OPRICH = RechercheOPRICH.getContainer()
      OPRICH.set("OJCONO",currentCompany)
      OPRICH.set("OJCUNO","")
      OPRICH.set("OJPRRF",prrf)
      OPRICH.set("OJCUCD",OAGRLN.get("UWCUCD").toString())
      RechercheOPRICH.readAll(OPRICH,4,{ DBContainer OPRICHresult ->

        int oagrlnStdt = OAGRLN.get("UWSTDT") as Integer
        int oprichFvdt = OPRICHresult.get("OJFVDT") as Integer
        if( oagrlnStdt >= oprichFvdt){
          if((tarifDate != 0 && tarifDate < oprichFvdt) || tarifDate == 0){
            tarifDate = OPRICHresult.get("OJFVDT") as Integer
          }
        }
      })
      logger.debug("Date Tarif: "+ tarifDate)


      def params = ["PRRF": prrf, "CUCD": OAGRLN.get("UWCUCD").toString(), "FVDT": String.valueOf(tarifDate),"ITNO": OAGRLN.get("UWOBV1").toString()]
      Closure<?> closure = {Map<String, String> response ->
        logger.debug("Response = ${response}")
        if(response.error == null){
          sapr = response.SAPR
          sacdOIS017 = response.SACD as double
        }else{
          sapr = "0.00"
        }
      }
      miCaller.call("OIS017MI", "GetBasePrice", params, closure)
      if(sacdOIS017>0){
        double saprValue = (sapr as double)*sacdOIS017
        mi.outData.put("SAPR",saprValue as String)
      }else{
        mi.outData.put("SAPR",sapr as String)
      }


      if(sacdOIS017>0){
        sapr = (sapr as double)*sacdOIS017
      }
      String reqt=""
      String dlqt=""

      logger.debug("CUNO = " + cuno)
      logger.debug("AGNO = " + agno)
      logger.debug("FDAT = " + OAGRLN.get("UWFDAT").toString())
      logger.debug("PREX = " + OAGRLN.get("UWPREX").toString())
      logger.debug("STDT = " + OAGRLN.get("UWSTDT").toString())
      logger.debug("OBV1 = " + OAGRLN.get("UWOBV1").toString())
      def params2 = ["CUNO":cuno, "AGNO":agno,"FDAT":OAGRLN.get("UWFDAT").toString()+"","PREX":OAGRLN.get("UWPREX").toString(),"STDT":OAGRLN.get("UWSTDT").toString()+"","OBV1":OAGRLN.get("UWOBV1").toString()]
      Closure<?> closure2 = {Map<String, String> response ->
        logger.debug("Response = ${response}")
        if(response.error == null){
          reqt = response.REQT
          dlqt = response.DLQT
        }else{
          reqt = "0.00"
          dlqt = "0.00"
        }
      }
      miCaller.call("OIS060MI", "GetCustBlkAgrLn", params2, closure2)


      mi.outData.put("REQT", reqt)
      mi.outData.put("DLQT", dlqt)
      mi.write()
    }
  }
}
