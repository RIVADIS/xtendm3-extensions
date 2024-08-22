/**
 * README
 * This extension is used by Mashup
 *
 * Name : EXT061MI.AddCustBlkAgrLn
 * Description : The AddCustBlkAgrLn transaction adds records to the EXT061 table.
 * Date         Changed By   Description
 * 20210827     M3INFORARE   COMX01 - Management of customer agreement
 * 20220128     M3INFORARE   logger.info and end of line semicolon have been removed
 * 20220217     MBEN         Description of input/output parameters modified
 */
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
public class AddCustBlkAgrLn extends ExtendM3Transaction {
  private final MIAPI mi
  private final DatabaseAPI database
  private final LoggerAPI logger
  private final MICallerAPI miCaller
  private final ProgramAPI program
  private final UtilityAPI utility
  private String cuno = ""
  private String agno = ""
  private String fdat =""
  private String stdt =""
  private String prex =""
  private String obv1 =""
  private String obv2 =""
  private String obv3 =""
  private String obv4 =""
  private String zlot =""
  private String tx40 =""
  private String zslo =""
  private Double zqts = 0
  private String zuns =""
  private Integer ztre = 0
  private String stac =""

  public AddCustBlkAgrLn(MIAPI mi, DatabaseAPI database, LoggerAPI logger, ProgramAPI program,UtilityAPI utility) {
    this.mi = mi
    this.database = database
    this.logger = logger
    this.program = program
    this.utility = utility
  }

  public void main() {
    Integer currentCompany
    if (mi.in.get("CONO") == null) {
      currentCompany = (Integer)program.getLDAZD().CONO
    } else {
      currentCompany = mi.in.get("CONO")
    }

    if (mi.in.get("CUNO") != null){
      cuno = mi.in.get("CUNO")
      DBAction countryQuery = database.table("OCUSMA").index("00").build()
      DBContainer OCUSMA = countryQuery.getContainer()
      OCUSMA.set("OKCONO",currentCompany)
      OCUSMA.set("OKCUNO",mi.in.get("CUNO"))
      if (!countryQuery.read(OCUSMA)) {
        mi.error("Code client " + mi.in.get("CUNO") + " n'existe pas")
        return
      }
    } else {
      mi.error("Code client est obligatoire")
      return
    }
    if (mi.in.get("AGNO") != null){
      agno = mi.in.get("AGNO")
    } else {
      mi.error("Numéro contrat est obligatoire")
      return
    }
    if (mi.in.get("FDAT") != null){
      fdat = mi.in.get("FDAT")
      if (!utility.call("DateUtil", "isDateValid", fdat, "yyyyMMdd")) {
        mi.error("Date début " + mi.in.get("FDAT") + " est invalide (FDAT)")
        return
      }
    } else {
      mi.error("Date début est obligatoire (FDAT)")
      return
    }
    if (mi.in.get("STDT") != null){
      stdt = mi.in.get("STDT")
      if (!utility.call("DateUtil", "isDateValid", stdt, "yyyyMMdd")) {
        mi.error("Date début " + mi.in.get("STDT") + " est invalide (STDT)")
        return
      }
    } else {
      mi.error("Date début est obligatoire (STDT)")
      return
    }
    if (mi.in.get("PREX") != null){
      if(mi.in.get("PREX") == "1" || mi.in.get("PREX") == " 1") prex = " 1"
      if(mi.in.get("PREX") == "2" || mi.in.get("PREX") == " 2") prex = " 2"
      if(mi.in.get("PREX") == "3" || mi.in.get("PREX") == " 3") prex = " 3"
      if(mi.in.get("PREX") == "4" || mi.in.get("PREX") == " 4") prex = " 4"
      if(mi.in.get("PREX") == "5" || mi.in.get("PREX") == " 5") prex = " 5"
      if(mi.in.get("PREX") == "6" || mi.in.get("PREX") == " 6") prex = " 6"
      if(mi.in.get("PREX") == "7" || mi.in.get("PREX") == " 7") prex = " 7"
      if(mi.in.get("PREX") == "8" || mi.in.get("PREX") == " 8") prex = " 8"
      if(mi.in.get("PREX") == "9" || mi.in.get("PREX") == " 9") prex = " 9"
      if (prex != " 1" && prex != " 2" && prex != " 3" && prex != " 4" && prex != " 5" && prex != " 6" && prex != " 7" && prex != " 8" && prex != " 9" && prex != "10") {
        mi.error("Priorité " + mi.in.get("PREX") + " est invalide")
        return
      }
    } else {
      mi.error("Priorité est obligatoire")
      return
    }
    if (mi.in.get("OBV1") != null){
      obv1 = mi.in.get("OBV1")
    } else {
      mi.error("Objet 1 est obligatoire")
      return
    }
    if (mi.in.get("OBV2") != null){
      obv2 = mi.in.get("OBV2")
    }
    if (mi.in.get("OBV3") != null){
      obv3 = mi.in.get("OBV3")
    }
    if (mi.in.get("OBV4") != null){
      obv4 = mi.in.get("OBV4")
    }
    if (mi.in.get("ZLOT") != null){
      zlot = mi.in.get("ZLOT")
    }
    if (mi.in.get("TX40") != null){
      tx40 = mi.in.get("TX40")
    }
    if (mi.in.get("ZSLO") != null){
      zslo = mi.in.get("ZSLO")
    }
    if (mi.in.get("ZQTS") != null){
      zqts = mi.in.get("ZQTS") as Double
    }
    if (mi.in.get("ZUNS") != null){
      zuns = mi.in.get("ZUNS")
    }
    if (mi.in.get("ZTRE") != null){
      ztre = mi.in.get("ZTRE") as Integer
    }
    if (mi.in.get("STAC") != null){
      stac = mi.in.get("STAC")
    }


    LocalDateTime timeOfCreation = LocalDateTime.now()
    DBAction query = database.table("OAGRLN").index("00").build()
    DBContainer OAGRLN = query.getContainer()
    OAGRLN.set("UWCONO", currentCompany)
    OAGRLN.set("UWCUNO", cuno)
    OAGRLN.set("UWAGNO", agno)
    OAGRLN.setInt("UWFDAT", fdat as Integer)
    OAGRLN.setInt("UWSTDT", stdt as Integer)
    OAGRLN.set("UWPREX", prex)
    OAGRLN.set("UWOBV1", obv1)
    OAGRLN.set("UWOBV2", obv2)
    OAGRLN.set("UWOBV3", obv3)
    OAGRLN.set("UWOBV4", obv4)
    if (query.read(OAGRLN)) {
      DBAction query2 = database.table("EXT061").index("00").build()
      DBContainer EXT061 = query2.getContainer()
      EXT061.set("EXCONO", currentCompany)
      EXT061.set("EXCUNO", cuno)
      EXT061.set("EXAGNO", agno)
      EXT061.setInt("EXFDAT", fdat as Integer)
      EXT061.setInt("EXSTDT", stdt as Integer)
      EXT061.set("EXPREX", prex)
      EXT061.set("EXOBV1", obv1)
      EXT061.set("EXOBV2", obv2)
      EXT061.set("EXOBV3", obv3)
      EXT061.set("EXOBV4", obv4)
      if (!query2.read(EXT061)) {
        EXT061.set("EXZLOT", zlot)
        EXT061.set("EXTX40", tx40)
        EXT061.set("EXZSLO", zslo)
        EXT061.set("EXZQTS", zqts)
        EXT061.set("EXZUNS", zuns)
        EXT061.setInt("EXZTRE", ztre as Integer)
        EXT061.setInt("EXRGDT", timeOfCreation.format(DateTimeFormatter.ofPattern("yyyyMMdd")) as Integer)
        EXT061.setInt("EXRGTM", timeOfCreation.format(DateTimeFormatter.ofPattern("HHmmss")) as Integer)
        EXT061.setInt("EXLMDT", timeOfCreation.format(DateTimeFormatter.ofPattern("yyyyMMdd")) as Integer)
        EXT061.setInt("EXCHNO", 1)
        EXT061.set("EXCHID", program.getUser())
        EXT061.set("EXSTAC", stac)
        query2.insert(EXT061)
      } else {
        EXT061.set("EXCONO", currentCompany)
        EXT061.set("EXCUNO", cuno)
        EXT061.set("EXAGNO", agno)
        EXT061.setInt("EXFDAT", fdat as Integer)
        EXT061.setInt("EXSTDT", stdt as Integer)
        EXT061.set("EXPREX", prex)
        EXT061.set("EXOBV1", obv1)
        EXT061.set("EXOBV2", obv2)
        EXT061.set("EXOBV3", obv3)
        EXT061.set("EXOBV4", obv4)
        if(!query2.readLock(EXT061, updateCallBack)){}
      }
    } else {
      mi.error("Ligne de contrat n'existe pas")
      return
    }
  }
  Closure<?> updateCallBack = { LockedResult lockedResult ->
    LocalDateTime timeOfCreation = LocalDateTime.now()
    int changeNumber = lockedResult.get("EXCHNO")
    if (mi.in.get("ZLOT") != null)
      lockedResult.set("EXZLOT", zlot)
    if (mi.in.get("TX40") != null)
      lockedResult.set("EXTX40", tx40)
    if (mi.in.get("ZSLO") != null)
      lockedResult.set("EXZSLO", zslo)
    if (mi.in.get("ZQTS") != null)
      lockedResult.set("EXZQTS", zqts)
    if (mi.in.get("ZUNS") != null)
      lockedResult.set("EXZUNS", zuns)
    if (mi.in.get("ZTRE") != null)
      lockedResult.setInt("EXZTRE", ztre as Integer)
    lockedResult.set("EXSTAC", stac)
    lockedResult.setInt("EXLMDT", timeOfCreation.format(DateTimeFormatter.ofPattern("yyyyMMdd")) as Integer)
    lockedResult.setInt("EXCHNO", changeNumber + 1)
    lockedResult.set("EXCHID", program.getUser())
    lockedResult.update()
  }
}
