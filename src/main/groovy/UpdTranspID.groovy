/*
class UpdTranspID {
}
*/

/**
 * README
 * This extension is used by Mashup
 *
 * Name : EXT410MI.UpdTranspID
 * Description : Update delivery header transporation Service ID.
 * Date         Changed By   Description
 * 20240908     LAISYL       LOGX05 - Update Transportation Service ID see infor case CS0248241
 */
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

public class UpdTranspID extends ExtendM3Transaction {
  private final MIAPI mi
  private final LoggerAPI logger
  private final ProgramAPI program
  private final DatabaseAPI database
  private final SessionAPI session
  private final TransactionAPI transaction
  private final MICallerAPI miCaller
  private final UtilityAPI utility
  private Integer currentCompany
  private Integer conn
  private String pgrs

  public UpdTranspID(MIAPI mi, DatabaseAPI database, ProgramAPI program, MICallerAPI miCaller, UtilityAPI utility) {
    this.mi = mi
    this.database = database
    this.program = program
    this.miCaller = miCaller
    this.utility = utility
  }

  public void main() {

    if (mi.in.get("CONO") == null) {
      currentCompany = (Integer)program.getLDAZD().CONO
    } else {
      currentCompany = mi.in.get("CONO")
    }

    // Check delivery index
    if (mi.in.get("DLIX") == null || mi.in.get("DLIX") == "") {
      mi.error("Index de livraison est obligatoire")
      return
    }

    // Check reason code
    if(mi.in.get("TSID") != null && mi.in.get("TSID") != ""){
      DBAction query = database.table("DRTRSR").index("00").build()
      DBContainer DRTRSR = query.getContainer()
      DRTRSR.set("DTCONO",currentCompany)
      DRTRSR.set("DTTSID",  mi.in.get("TSID"))
      if (!query.read(DRTRSR)) {
        mi.error("Service Transport ID " + mi.in.get("TSID") + " n'existe pas")
        return
      }
    }

    // Check delivery
    DBAction query = database.table("MHDISH").index("00").build()
    DBContainer MHDISH = query.getContainer()
    MHDISH.set("OQCONO", currentCompany)
    MHDISH.set("OQINOU",  1)
    MHDISH.set("OQDLIX",  mi.in.get("DLIX"))
    if(!query.readLock(MHDISH, updateCallBack)){
      mi.error("L'index de livraison " + mi.in.get("DLIX") + " n'existe pas")
      return
    }
  }
  Closure<?> updateCallBack = { LockedResult lockedResult ->
    LocalDateTime timeOfCreation = LocalDateTime.now()
    int changeNumber = lockedResult.get("OQCHNO")
    lockedResult.set("OQTSID", mi.in.get("TSID"))
    lockedResult.setInt("OQLMDT", timeOfCreation.format(DateTimeFormatter.ofPattern("yyyyMMdd")) as Integer)
    lockedResult.setInt("OQCHNO", changeNumber + 1)
    lockedResult.set("OQCHID", program.getUser())
    lockedResult.update()
  }
}
