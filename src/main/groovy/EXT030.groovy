/**
 * README
 * This extension is used by Mashup
 *
 * Name : EXT030 Batch
 * Description : The SwitchItems switch article agreement line.
 * Date         Changed By   Description
 * 20220128     APACE        COMX01 - Management of customer agreement
 * 20240205     MLECLERCQ    COMX01 - Correction switch date on source item line
 * 20240221     MLECLERCQ    COMX01 - def transformed to Map<String,String>
 * 20240523     MLECLERCQ    COMX01 - corrected customer filter
 * 20240528     MLECLERCQ    COMX01 - get ZLOT and ZSLO from old item to new item
 * 20240528     MLECLERCQ    COMX01 - corrected lines date validity
 */


import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import groovy.json.JsonBuilder
public class EXT030 extends ExtendM3Batch {

  private final DatabaseAPI database
  private final LoggerAPI logger
  private final MICallerAPI miCaller
  private final ProgramAPI program
  private final UtilityAPI utility
  private final BatchAPI batch


  String rawData
  int rawDataLength
  int beginIndex
  int endIndex
  public int count = 0
  public int error = 0
  int currentCompany = 0
  String USID = ""
  String itno = ""
  String itnz = ""
  String cuno = ""
  String appl = ""
  int lvdt = 0
  String agtp = ""
  Double agp1=0
  Double agp2=0
  String agno = ""
  DBContainer saveOAGRLN
  int returnRecords = 10000

  String current_agtp = ""



  public EXT030( DatabaseAPI database, LoggerAPI logger, ProgramAPI program,UtilityAPI utility, MICallerAPI miCaller,BatchAPI batch) {
    this.database = database
    this.logger = logger
    this.program = program
    this.utility = utility
    this.miCaller = miCaller
    this.batch = batch
  }

  public void main() {
    if(batch.getReferenceId().isPresent()){
      Optional<String> data = getJobData(batch.getReferenceId().get())
      logger.debug("ID jobs: "+batch.getReferenceId().get())
      switchItem(data)
    } else {
      // No job data found
      logger.debug("Job data for job ${batch.getJobId()} is missing")
    }
  }

  // Get job data
  private Optional<String> getJobData(String referenceId){
    DBAction query = database.table("EXTJOB").index("00").selection("EXDATA").build()
    DBContainer container = query.createContainer()
    container.set("EXRFID", referenceId)
    if (query.read(container)){
      return Optional.of(container.getString("EXDATA"))
    } else {
      logger.debug("EXTJOB not found")
    }
    return Optional.empty()
  }

  // Cost price calcul
  private switchItem(Optional<String> data){
    if(!data.isPresent()){
      logger.debug("Job reference Id ${batch.getReferenceId().get()} is passed but data was not found")
      return
    }
    rawData = data.get()
    currentCompany = (Integer)program.getLDAZD().CONO
    USID = program.getUser()
    itno = getFirstParameter()
    itnz = getNextParameter()
    cuno = getNextParameter()
    agtp = getNextParameter()

    String lvdtSave = getNextParameter()
    if(lvdtSave!=""){
      lvdt = lvdtSave as Integer
    }
    String agp1Save = getNextParameter()
    if(agp1Save!=""){
      agp1 = agp1Save as Double
    }
    String agp2Save = getNextParameter()
    if(agp2Save!=""){
      agp2 = agp2Save as Double
    }
    appl = getNextParameter()

    if((agp1==0 && agp2!=0)||(agp1!=0 && agp2==0)){
      logger.debug("Le prix source/cible obligatoires!")
      return
    }

    logger.debug("itno: "+itno)
    logger.debug("itnz: "+itnz)
    logger.debug("cuno: "+cuno)
    logger.debug("agtp: "+agtp)
    logger.debug("lvdt: "+lvdt)
    logger.debug("agp1: "+agp1)
    logger.debug("agp2: "+agp2)
    logger.debug("appl: "+appl)

    dellLineErrorEXT830(appl)

    ExpressionFactory expression = database.getExpressionFactory("OAGRLN")

    if(!itno.isBlank() && lvdt!=0  && cuno.isBlank()){
      expression = expression.eq("UWOBV1", itno).and(expression.ge("UWAGST", "00")).and(expression.le("UWAGST", "79"))
    }
    if(!itno.isBlank() && lvdt!=0 &&  !cuno.isBlank()){
      expression = expression.eq("UWOBV1", itno).and(expression.eq("UWCUNO", cuno)).and(expression.ge("UWAGST", "00")).and(expression.le("UWAGST", "79"))
    }


    DBAction rechercheOAGRLN = database.table("OAGRLN").index("00").matching(expression).selection( "UWCUNO","UWAGNO","UWFDAT","UWPREX","UWOBV1","UWOBV2","UWOBV3","UWOBV4","UWSTDT","UWLVDT","UWPRRF","UWPRLC","UWSPUN","UWSUNO","UWAGNB","UWAGQT","UWUNIT","UWD2QT","UWD3QT","UWNAQT","UWAGLN").reverse().build()

    DBContainer OAGRLN = rechercheOAGRLN.getContainer()
    OAGRLN.set("UWCONO", currentCompany)
    //Read All  Item's Contrat
    logger.debug("Read OAGRLN:")
    rechercheOAGRLN.readAll(OAGRLN, 1, {DBContainer resultOAGRLN ->
      saveOAGRLN = resultOAGRLN
      String currentCuno = resultOAGRLN.get("UWCUNO").toString()
      if(selection_isOK(agtp,resultOAGRLN)){
        if(article_cible_inexistant(resultOAGRLN)){
          String stdt = resultOAGRLN.get("UWSTDT").toString()
          String stdt2 = ""
          if((resultOAGRLN.get("UWSTDT").toString() as int) < lvdt){
            stdt = lvdt
          }
          if(lvdt < (resultOAGRLN.get("UWFDAT").toString() as int)){
            stdt = resultOAGRLN.get("UWFDAT").toString()
          }

          boolean controle = false;
          DBAction rechercheOAGRPRControle = database.table("OAGRPR").index("00").selection("OLCUNO","OLAGNO","OLFDAT",
            "OLPREX","OLOBV1","OLOBV2","OLOBV3","OLOBV4","OLSTDT","OLQTYL","OLAGPR","OLSACD","OLDIPC",
            "OLDIPR").build()
          DBContainer controleOAGRPR = rechercheOAGRPRControle.getContainer()

          controleOAGRPR.set("OLCONO", currentCompany)
          controleOAGRPR.set("OLCUNO", resultOAGRLN.get("UWCUNO").toString())
          controleOAGRPR.set("OLAGNO", resultOAGRLN.get("UWAGNO").toString())
          controleOAGRPR.set("OLFDAT", resultOAGRLN.get("UWFDAT").toString() as Integer)
          controleOAGRPR.set("OLSTDT", resultOAGRLN.get("UWSTDT").toString() as Integer)
          if(lvdt < (resultOAGRLN.get("UWFDAT").toString() as Integer)){
            controleOAGRPR.set("OLSTDT", resultOAGRLN.get("UWFDAT").toString() as Integer)
          }
          controleOAGRPR.set("OLPREX", resultOAGRLN.get("UWPREX").toString())
          controleOAGRPR.set("OLOBV1", resultOAGRLN.get("UWOBV1").toString())
          controleOAGRPR.set("OLOBV2", resultOAGRLN.get("UWOBV2").toString())
          controleOAGRPR.set("OLOBV3", resultOAGRLN.get("UWOBV3").toString())
          controleOAGRPR.set("OLOBV4", resultOAGRLN.get("UWOBV4").toString())
          rechercheOAGRPRControle.readAll(controleOAGRPR, 10,returnRecords, {DBContainer controleResultOAGRPR ->
            stdt2 =  resultOAGRLN.get("UWSTDT").toString()
            logger.debug("OAPRPR control, stdt2 = " + stdt2 + ", lvdt = " + lvdt +", OLSTDT = " + controleResultOAGRPR.get("OLSTDT").toString() + ", OLFDAT = " + controleResultOAGRPR.get("OLFDAT").toString())
            if(lvdt > (controleResultOAGRPR.get("OLSTDT").toString() as int)){
              stdt2 = lvdt
            }
            if(lvdt < (controleResultOAGRPR.get("OLFDAT").toString() as int)){
              stdt2 = resultOAGRLN.get("UWFDAT").toString()
            }

            Double sapr = controleResultOAGRPR.get("OLAGPR").toString() as Double
            Double SACD = controleResultOAGRPR.get("OLSACD").toString() as Double
            if(SACD==0){
              SACD = 1
            }
            Double calculSAPR = sapr*SACD
            DecimalFormat format = new DecimalFormat("0.##")

            Map<String,String> paramsAddAgrLnPrice
            if(agp2!=0 && agp1!=0){
              if(controleResultOAGRPR.get("OLDIPC").toString()=="" && format.format(calculSAPR)==format.format(agp1)){
                controle = true;
              }
              if(controleResultOAGRPR.get("OLDIPC").toString()!="" && format.format(calculSAPR)==format.format(agp1)){
                controle = true;
              }
            }else{
              controle = true;
            }
          })

          if(controle){
            agno = resultOAGRLN.get("UWAGNO").toString()

            Map<String,String> paramsAddCustBlkAgrLn  = ["CUNO":resultOAGRLN.get("UWCUNO").toString(),"AGNO":resultOAGRLN.get("UWAGNO").toString(),
                                                         "FDAT":resultOAGRLN.get("UWFDAT").toString(),"PREX":resultOAGRLN.get("UWPREX").toString(),
                                                         "OBV1":itnz,"OBV2":resultOAGRLN.get("UWOBV2").toString(),
                                                         "OBV3":resultOAGRLN.get("UWOBV3").toString(),"OBV4":resultOAGRLN.get("UWOBV4").toString(),
                                                         "STDT":stdt,"LVDT":resultOAGRLN.get("UWLVDT").toString(),
                                                         "PRRF":resultOAGRLN.get("UWPRRF").toString(),"PRLC":resultOAGRLN.get("UWPRLC").toString(),
                                                         "SPUN":resultOAGRLN.get("UWSPUN").toString(),"SUNO":resultOAGRLN.get("UWSUNO").toString(),
                                                         "AGNB":resultOAGRLN.get("UWAGNB").toString(),"AGQT":resultOAGRLN.get("UWAGQT").toString(),
                                                         "UNIT":resultOAGRLN.get("UWUNIT").toString(),"D2QT":resultOAGRLN.get("UWD2QT").toString(),
                                                         "D3QT":resultOAGRLN.get("UWD3QT").toString(),"NAQT":resultOAGRLN.get("UWNAQT").toString()]
            boolean valid = true
            Closure<?> closureAddCustBlkAgrLn  = {Map<String, String> response ->
              logger.debug("Response = ${response}")
              if(response.errorMessage){
                addLineErrorEXT830(appl,USID,"OIS060MI", "AddCustBlkAgrLn", paramsAddCustBlkAgrLn, response.errorMessage)
                valid = false
                error = 1
              }
            }

            miCaller.call("OIS060MI", "AddCustBlkAgrLn", paramsAddCustBlkAgrLn , closureAddCustBlkAgrLn )

            DBAction rechercheOAGRPR = database.table("OAGRPR").index("00").selection("OLCUNO","OLAGNO","OLFDAT",
              "OLPREX","OLOBV1","OLOBV2","OLOBV3","OLOBV4","OLSTDT","OLQTYL","OLAGPR","OLSACD","OLDIPC",
              "OLDIPR").build()
            DBContainer OAGRPR = rechercheOAGRPR.getContainer()

            OAGRPR.set("OLCONO", currentCompany)
            OAGRPR.set("OLCUNO", resultOAGRLN.get("UWCUNO").toString())
            OAGRPR.set("OLAGNO", resultOAGRLN.get("UWAGNO").toString())
            OAGRPR.set("OLFDAT", resultOAGRLN.get("UWFDAT").toString() as Integer)
            OAGRPR.set("OLSTDT", resultOAGRLN.get("UWSTDT").toString() as Integer)
            if(lvdt < (resultOAGRLN.get("UWFDAT").toString() as Integer)){
              OAGRPR.set("OLSTDT", resultOAGRLN.get("UWFDAT").toString() as Integer)
            }
            OAGRPR.set("OLPREX", resultOAGRLN.get("UWPREX").toString())
            OAGRPR.set("OLOBV1", resultOAGRLN.get("UWOBV1").toString())
            OAGRPR.set("OLOBV2", resultOAGRLN.get("UWOBV2").toString())
            OAGRPR.set("OLOBV3", resultOAGRLN.get("UWOBV3").toString())
            OAGRPR.set("OLOBV4", resultOAGRLN.get("UWOBV4").toString())
            rechercheOAGRPR.readAll(OAGRPR, 10, {DBContainer resultOAGRPR ->
              stdt2 =  resultOAGRLN.get("UWSTDT").toString()
              if(lvdt > (resultOAGRPR.get("OLSTDT").toString() as int)){
                stdt2 = lvdt
              }
              if(lvdt < (resultOAGRPR.get("OLFDAT").toString() as int)){
                stdt2 = resultOAGRLN.get("UWFDAT").toString()
              }
              Double sapr = resultOAGRPR.get("OLAGPR").toString() as Double
              Double SACD = resultOAGRPR.get("OLSACD").toString() as Double
              if(SACD==0){
                SACD = 1
              }
              Double calculSAPR = sapr*SACD
              DecimalFormat format = new DecimalFormat("0.##")

              Map<String,String> paramsAddAgrLnPrice
              if(agp2!=0 && agp1!=0){
                if(resultOAGRPR.get("OLDIPC").toString()=="" && format.format(calculSAPR)==format.format(agp1)){
                  paramsAddAgrLnPrice = [
                    "CUNO":resultOAGRPR.get("OLCUNO").toString(),
                    "AGNO":resultOAGRPR.get("OLAGNO").toString(),
                    "FDAT":resultOAGRPR.get("OLFDAT").toString(),
                    "PREX":resultOAGRPR.get("OLPREX").toString(),
                    "OBV1":itnz,
                    "OBV2":resultOAGRPR.get("OLOBV2").toString(),
                    "OBV3":resultOAGRPR.get("OLOBV3").toString(),
                    "OBV4":resultOAGRPR.get("OLOBV4").toString(),
                    "STDT":stdt2,
                    "QTYL":resultOAGRPR.get("OLQTYL").toString(),
                    "SAPR":format.format(agp2)+"",
                    "SACD":resultOAGRPR.get("OLSACD").toString()
                  ]
                  Closure<?> closureAddAgrLnPrice  = {Map<String, String> response ->
                    logger.debug("Response = ${response}")
                    addLineErrorEXT830(appl,USID,"OIS060MI", "AddAgrLnPrice", paramsAddAgrLnPrice, response.errorMessage)
                    error = 1
                  }
                  miCaller.call("OIS060MI", "AddAgrLnPrice", paramsAddAgrLnPrice , closureAddAgrLnPrice )
                }
                if(resultOAGRPR.get("OLDIPC").toString()!="" && format.format(calculSAPR)==format.format(agp1)){

                  paramsAddAgrLnPrice = [
                    "CUNO":resultOAGRPR.get("OLCUNO").toString(),
                    "AGNO":resultOAGRPR.get("OLAGNO").toString(),
                    "FDAT":resultOAGRPR.get("OLFDAT").toString(),
                    "PREX":resultOAGRPR.get("OLPREX").toString(),
                    "OBV1":itnz,
                    "OBV2":resultOAGRPR.get("OLOBV2").toString(),
                    "OBV3":resultOAGRPR.get("OLOBV3").toString(),
                    "OBV4":resultOAGRPR.get("OLOBV4").toString(),
                    "STDT":stdt2,
                    "QTYL":resultOAGRPR.get("OLQTYL").toString(),
                    "SAPR":format.format(agp2)+"",
                    "SACD":resultOAGRPR.get("OLSACD").toString(),
                    "DIPC":resultOAGRPR.get("OLDIPC").toString()
                  ]
                  Closure<?> closureAddAgrLnPrice  = {Map<String, String> response ->
                    logger.debug("Response = ${response}")
                    addLineErrorEXT830(appl,USID,"OIS060MI", "AddAgrLnPrice", paramsAddAgrLnPrice, response.errorMessage)
                    error = 1
                  }
                  miCaller.call("OIS060MI", "AddAgrLnPrice", paramsAddAgrLnPrice , closureAddAgrLnPrice )
                }
              }else{
                if(resultOAGRPR.get("OLDIPC").toString()==""){
                  paramsAddAgrLnPrice  = [
                    "CUNO":resultOAGRPR.get("OLCUNO").toString(),
                    "AGNO":resultOAGRPR.get("OLAGNO").toString(),
                    "FDAT":resultOAGRPR.get("OLFDAT").toString(),
                    "PREX":resultOAGRPR.get("OLPREX").toString(),
                    "OBV1":itnz,
                    "OBV2":resultOAGRPR.get("OLOBV2").toString(),
                    "OBV3":resultOAGRPR.get("OLOBV3").toString(),
                    "OBV4":resultOAGRPR.get("OLOBV4").toString(),
                    "STDT":stdt2,
                    "QTYL":resultOAGRPR.get("OLQTYL").toString(),
                    "SAPR":format.format(calculSAPR)+"",
                    "SACD":resultOAGRPR.get("OLSACD").toString()
                  ]
                }else{
                  paramsAddAgrLnPrice  = [
                    "CUNO":resultOAGRPR.get("OLCUNO").toString(),
                    "AGNO":resultOAGRPR.get("OLAGNO").toString(),
                    "FDAT":resultOAGRPR.get("OLFDAT").toString(),
                    "PREX":resultOAGRPR.get("OLPREX").toString(),
                    "OBV1":itnz,
                    "OBV2":resultOAGRPR.get("OLOBV2").toString(),
                    "OBV3":resultOAGRPR.get("OLOBV3").toString(),
                    "OBV4":resultOAGRPR.get("OLOBV4").toString(),
                    "STDT":stdt2,
                    "QTYL":resultOAGRPR.get("OLQTYL").toString(),
                    "SAPR":format.format(calculSAPR)+"",
                    "SACD":resultOAGRPR.get("OLSACD").toString(),
                    "DIPC":resultOAGRPR.get("OLDIPC").toString()
                  ]
                }
                Closure<?> closureAddAgrLnPrice  = {Map<String, String> response ->
                  logger.debug("Response = ${response}")
                  addLineErrorEXT830(appl,USID,"OIS060MI", "AddAgrLnPrice", paramsAddAgrLnPrice, response.errorMessage)
                  error = 1
                }
                miCaller.call("OIS060MI", "AddAgrLnPrice", paramsAddAgrLnPrice , closureAddAgrLnPrice )
              }
            })
            if(valid){
              logger.debug("UPDATE OAGRLN OK!")

              Integer temp_lvdt = lvdt
              DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
              LocalDate localDate

              if(lvdt < (resultOAGRLN.get("UWSTDT").toString() as int) ){
                temp_lvdt = (resultOAGRLN.get("UWSTDT").toString() as int )
                localDate = LocalDate.parse(''+temp_lvdt,formatter)
              }else{
                localDate = LocalDate.parse(''+temp_lvdt,formatter)
                localDate = localDate.minusDays(1)
              }

              String target_lvdt = localDate.format(formatter) as String
              Map<String,String> paramsOIS060 = ["CUNO":resultOAGRLN.get("UWCUNO").toString().trim()
                                                 ,"AGNO":resultOAGRLN.get("UWAGNO").toString().trim()
                                                 ,"FDAT":resultOAGRLN.get("UWFDAT").toString().trim()
                                                 ,"STDT":resultOAGRLN.get("UWSTDT").toString().trim()
                                                 ,"PREX":resultOAGRLN.get("UWPREX").toString()
                                                 ,"OBV1":resultOAGRLN.get("UWOBV1").toString().trim()
                                                 ,"OBV2":resultOAGRLN.get("UWOBV2").toString()
                                                 ,"OBV3":resultOAGRLN.get("UWOBV3").toString()
                                                 ,"OBV4":resultOAGRLN.get("UWOBV4").toString()
                                                 ,"LVDT":target_lvdt]
              Closure<?> closureOIS060 = {Map<String, String> response ->
                if(response.errorMessage){
                  addLineErrorEXT830(appl,USID,"OIS060MI", "UpdCustBlkAgrLn", paramsOIS060, response.errorMessage)
                  error = 1
                }
              }
              miCaller.call("OIS060MI", "UpdCustBlkAgrLn", paramsOIS060, closureOIS060)

              //get OIS061 record ZLOT and ZSLO then add one record with them for new itno and new date
              DBAction rechercheEXT061 = database.table("EXT061").index("00").selection("EXCUNO","EXAGNO","EXFDAT",
                "EXSTDT","EXPREX","EXOBV1","EXOBV2","EXOBV3","EXOBV4","EXZLOT","EXTX40","EXZSLO","EXZQTS",
                "EXZTRE","EXSTAC","EXPRRF","EXP001","EXP002","EXP003","EXP004","EXP005","EXP006","EXP007","EXP008"
                ,"EXP009","EXP010").build()
              DBContainer containerEXT061 = rechercheEXT061.getContainer()

              String oagrlnCuno = resultOAGRLN.get("UWCUNO").toString()
              String oagrlnAgno = resultOAGRLN.get("UWAGNO").toString()
              String oagrlnFdat = resultOAGRLN.get("UWFDAT").toString()
              String oagrlnStdt = resultOAGRLN.get("UWSTDT").toString()
              String oagrlnPrex = resultOAGRLN.get("UWPREX").toString()
              String oagrlnObv1 = itno


              containerEXT061.set("EXCONO", currentCompany)
              containerEXT061.set("EXCUNO",oagrlnCuno)
              containerEXT061.set("EXAGNO",oagrlnAgno)
              containerEXT061.set("EXFDAT",resultOAGRLN.get("UWFDAT").toString() as Integer)
              containerEXT061.set("EXSTDT",resultOAGRLN.get("UWSTDT").toString() as Integer)
              containerEXT061.set("EXPREX",oagrlnPrex)
              containerEXT061.set("EXOBV1",itno)

              logger.debug("Ready to check in EXT061 record : ${oagrlnCuno} | ${oagrlnAgno} | ${oagrlnFdat} | ${oagrlnStdt} | ${oagrlnPrex} | ${oagrlnObv1} ")

              rechercheEXT061.readAll(containerEXT061,7,returnRecords,{DBContainer resultEXT061 ->

                String ext061Cuno = resultEXT061.get("EXCUNO").toString()
                String ext061Agno = resultEXT061.get("EXAGNO").toString()
                String ext061Fdat = resultEXT061.get("EXFDAT").toString()
                String ext061Stdt = resultEXT061.get("EXSTDT").toString()
                String ext061Prex = resultEXT061.get("EXPREX").toString()
                String ext061Obv1 = resultEXT061.get("EXOBV1").toString()
                String ext061Zlot = resultEXT061.get("EXZLOT").toString()
                String ext061Zslo = resultEXT061.get("EXZSLO").toString()

                logger.debug("EXT061 Found : ${ext061Cuno} | ${ext061Agno} | ${ext061Fdat} | ${ext061Stdt} | ${ext061Prex} | ${ext061Obv1} | ${ext061Zlot} | ${ext061Zslo}")
                logger.debug("EXT061 while AGTP = ${current_agtp} and lvdt = ${lvdt.toString()} and stdt = ${stdt} and target_lvdt = ${target_lvdt} and stdt2 = ${stdt2} and OASTDT = ${resultOAGRLN.get("UWSTDT").toString()}" )

                String date
                if(lvdt.toString() < target_lvdt){
                  date = target_lvdt
                }else{
                  date = lvdt.toString()
                }

                Map<String,String> paramsEXT061 = [
                  "CUNO":resultEXT061.get("EXCUNO").toString()
                  ,"AGNO":resultEXT061.get("EXAGNO").toString()
                  ,"FDAT":resultEXT061.get("EXFDAT").toString()
                  ,"STDT":date
                  ,"PREX":resultEXT061.get("EXPREX").toString()
                  ,"OBV1":itnz.toString()
                  ,"OBV2":resultEXT061.get("EXOBV2").toString()
                  ,"OBV3":resultEXT061.get("EXOBV3").toString()
                  ,"OBV4":resultEXT061.get("EXOBV4").toString()
                  ,"ZLOT":resultEXT061.get("EXZLOT").toString()
                  ,"TX40":resultEXT061.get("EXTX40").toString()
                  ,"ZSLO":resultEXT061.get("EXZSLO").toString()
                  ,"ZQTS":resultEXT061.get("EXZQTS").toString()
                  ,"ZTRE":resultEXT061.get("EXZTRE").toString()
                  ,"STAC":resultEXT061.get("EXSTAC").toString()
                  ,"PRRF":resultEXT061.get("EXPRRF").toString()
                  ,"P001":resultEXT061.get("EXP001").toString()
                  ,"P002":resultEXT061.get("EXP002").toString()
                  ,"P003":resultEXT061.get("EXP003").toString()
                  ,"P004":resultEXT061.get("EXP004").toString()
                  ,"P005":resultEXT061.get("EXP005").toString()
                  ,"P006":resultEXT061.get("EXP006").toString()
                  ,"P007":resultEXT061.get("EXP007").toString()
                  ,"P008":resultEXT061.get("EXP008").toString()
                  ,"P009":resultEXT061.get("EXP009").toString()
                  ,"P010":resultEXT061.get("EXP010").toString()
                ]

                Closure<?> closureEXT061 = {Map<String, String> response ->
                  logger.debug("EXT061MI Response = ${response}")
                  addLineErrorEXT830(appl,USID,"EXT061MI", "AddCustBlkAgrLn", paramsEXT061, response.errorMessage)
                  error = 1
                }

                logger.debug("Ready to call in EXT061 to add : Lot = ${resultEXT061.get('EXZLOT').toString() } and Zslo: ${resultEXT061.get('EXZSLO').toString() } for ${ itnz.toString() } at FDAT : ${resultEXT061.get('EXFDAT').toString() } and STDT: ${date} ")
                miCaller.call("EXT061MI", "AddCustBlkAgrLn", paramsEXT061, closureEXT061)
              })

            }else{
              logger.debug("UPDATE OAGRLN KO!")
            }
          }else{
            logger.debug("Controle NOK")
          }
        }
      }
    })
    deleteEXTJOB()
    addLineErrorEXT830(appl,USID,"END", "END", null, "END")
  }

  //find item
  public article_cible_inexistant(DBContainer saveOAGRLN){
    boolean valid = false
    DBAction rechercheOAGRLN = database.table("OAGRLN").index("50").selection("UWAGST","UWLVDT","UWAGLN").build()
    DBContainer OAGRLN = rechercheOAGRLN.getContainer()
    OAGRLN.set("UWCONO",saveOAGRLN.get("UWCONO").toString() as Integer)
    if(cuno){
      OAGRLN.set("UWCUNO", cuno)
    }else{
      OAGRLN.set("UWCUNO",saveOAGRLN.get("UWCUNO").toString())
    }

    OAGRLN.set("UWAGNO",saveOAGRLN.get("UWAGNO").toString())
    OAGRLN.set("UWFDAT",saveOAGRLN.get("UWFDAT").toString() as Integer)
    OAGRLN.set("UWSTDT",saveOAGRLN.get("UWSTDT").toString() as Integer)
    OAGRLN.set("UWOBV1",itnz)
    logger.debug("Before ArticleCible inexistant, savedOAGRLN record = CUNO: ${cuno};AGNO:" + saveOAGRLN.get("UWAGNO").toString() + ", FDAT:" +saveOAGRLN.get("UWFDAT").toString() +",STDT:" +saveOAGRLN.get("UWFDAT").toString() +",OBV1:${itnz}, AGLN:"+saveOAGRLN.get("UWAGLN").toString()  )
    if(rechercheOAGRLN.readAll(OAGRLN,6,{ DBContainer resultOAGRLN ->
    })){}else{
      if( (saveOAGRLN.get("UWLVDT") as int) >= lvdt ){
        valid = true
      }
    }
    return valid
  }

  //find last contrat
  public lastOAGRLN(DBContainer saveOAGRLN){
    Integer result = 0
    DBAction rechercheOAGRLN = database.table("OAGRLN").index("40").reverse().selection("UWAGLN").build()
    DBContainer OAGRLN = rechercheOAGRLN.getContainer()
    OAGRLN.set("UWCONO",saveOAGRLN.get("UWCONO").toString() as Integer)
    OAGRLN.set("UWCUNO",saveOAGRLN.get("UWCUNO").toString())
    OAGRLN.set("UWAGNO",saveOAGRLN.get("UWAGNO").toString())
    OAGRLN.set("UWFDAT",saveOAGRLN.get("UWFDAT").toString() as Integer)
    rechercheOAGRLN.readAll(OAGRLN,4,1,{ DBContainer resultOAGRLN ->
      result = resultOAGRLN.get("UWAGLN").toString() as Integer
    })

    return result+1
  }

  //find customer agreement
  public selection_isOK(String AGTP,DBContainer OAGRLN){
    boolean valid = true
    if(AGTP!=""){
      DBAction rechercheOAGRHE = database.table("OAGRHE").index("00").selection("UYAGTP").build()
      DBContainer OAGRHE = rechercheOAGRHE.getContainer()
      OAGRHE.set("UYCONO",currentCompany)
      OAGRHE.set("UYCUNO",OAGRLN.get("UWCUNO").toString())
      OAGRHE.set("UYAGNO",OAGRLN.get("UWAGNO").toString())
      OAGRHE.set("UYSTDT",OAGRLN.get("UWFDAT").toString() as Integer)
      if(rechercheOAGRHE.read(OAGRHE)){
        current_agtp = OAGRHE.get("UYAGTP").toString().trim()
        logger.debug("Selection_isOK UYAGTP => "+OAGRHE.get("UYAGTP").toString() +" for UYAGNO:" + OAGRLN.get("UWAGNO").toString() + "and customer : " +OAGRLN.get("UWCUNO").toString() )
        if(!OAGRHE.get("UYAGTP").toString().trim().equals(AGTP.trim())){
          valid = false
        }else{
          valid = true
        }
      }else{
        current_agtp = ""
        valid = false
      }
    }else{
      current_agtp = ""
    }
    logger.debug("Selection_isOK is valid: "+valid)
    return valid
  }
  //delete all EXT830 (error history)
  private dellLineErrorEXT830(String APPL){
    LocalDateTime timeOfCreation = LocalDateTime.now()
    DBAction query = database.table("EXT830").index("00").build()
    DBContainer EXT830 = query.getContainer()
    EXT830.set("EXCONO",currentCompany)
    EXT830.set("EXUSID", USID)
    EXT830.set("EXAPPL", APPL)
    if(!query.readAllLock(EXT830, 3, updateCallBackDell)){
      logger.debug("L'enregistrement n'existe pas")
      return
    }
  }
  Closure<?> updateCallBackDell = { LockedResult lockedResult ->
    lockedResult.delete()
  }
  // add line error in EXT830 table (error history)
  private addLineErrorEXT830(String APPL,String USID,String PGRM, String TRAN, Map<String,String> PARM, String MSGE){
    LocalDateTime timeOfCreation = LocalDateTime.now()
    DBAction query = database.table("EXT830").index("00").build()
    DBContainer EXT830 = query.getContainer()
    EXT830.set("EXCONO",currentCompany)
    EXT830.set("EXUSID", USID)
    EXT830.set("EXAPPL", APPL)
    EXT830.setLong("EXLINE", count as Long)
    if (!query.read(EXT830)) {
      JsonBuilder builder = new JsonBuilder()
      if(PARM){
        builder(PARM)
        EXT830.set("EXPARM", builder.toString())
      }

      EXT830.set("EXPGRM", PGRM)
      EXT830.set("EXTRAN", TRAN)
      EXT830.set("EXMSGE", MSGE)

      EXT830.setInt("EXRGDT", timeOfCreation.format(DateTimeFormatter.ofPattern("yyyyMMdd")) as Integer)
      EXT830.setInt("EXRGTM", timeOfCreation.format(DateTimeFormatter.ofPattern("HHmmss")) as Integer)
      EXT830.setInt("EXLMDT", timeOfCreation.format(DateTimeFormatter.ofPattern("yyyyMMdd")) as Integer)
      EXT830.setInt("EXCHNO", 1)
      EXT830.set("EXCHID", program.getUser())
      //Add Error in EXT830
      query.insert(EXT830)
    }
    count++
  }

  // Get first parameter
  private String getFirstParameter(){
    logger.debug("rawData = " + rawData)
    rawDataLength = rawData.length()
    beginIndex = 0
    endIndex = rawData.indexOf(";")
    // Get parameter
    String parameter = rawData.substring(beginIndex, endIndex)
    logger.debug("parameter = " + parameter)
    return parameter
  }
  // Get next parameter
  private String getNextParameter(){
    beginIndex = endIndex + 1
    endIndex = rawDataLength - rawData.indexOf(";") - 1
    rawData = rawData.substring(beginIndex, rawDataLength)
    rawDataLength = rawData.length()
    beginIndex = 0
    endIndex = rawData.indexOf(";")
    // Get parameter
    String parameter = rawData.substring(beginIndex, endIndex)
    logger.debug("parameter = " + parameter)
    return parameter
  }

  // Delete records related to the current job from EXTJOB table
  public void deleteEXTJOB(){
    LocalDateTime timeOfCreation = LocalDateTime.now()
    DBAction query = database.table("EXTJOB").index("00").build()
    DBContainer EXTJOB = query.getContainer()
    EXTJOB.set("EXRFID", batch.getReferenceId().get())
    if(!query.readAllLock(EXTJOB, 1, updateCallBack)){
    }
  }
  // Delete record
  Closure<?> updateCallBack = { LockedResult lockedResult ->
    lockedResult.delete()
  }
}
