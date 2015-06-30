gradle jar
java -jar build/libs/org.mri-1.*.jar --classpath-file "../Axon-trader/this.classpath" -s ../Axon-trader/ -m createuser -f plantuml > createuser.puml
diff createuser.puml regression/createuser.puml
java -jar build/libs/org.mri-1.*.jar --classpath-file "../Axon-trader/this.classpath" -s ../Axon-trader/ -m createuser -f dot > createuser.dot
diff createuser.dot regression/createuser.dot
java -jar build/libs/org.mri-1.*.jar --classpath-file "../Axon-trader/this.classpath" -s ../Axon-trader/ -m CompanyController.sell -f plantuml > company_controller_sell.puml
diff company_controller_sell.puml regression/company_controller_sell.puml
java -jar build/libs/org.mri-1.*.jar --classpath-file "../Axon-trader/this.classpath" -s ../Axon-trader/ -m CompanyController.sell -f dot > company_controller_sell.dot
diff company_controller_sell.dot regression/company_controller_sell.dot
