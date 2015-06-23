gradle jar
java -jar build/libs/org.mri-1.*.jar --classpath-file "../Axon-trader/this.classpath" -s ../Axon-trader/ -m createuser -f plantuml > createuser.puml
diff createuser.puml regression/createuser.puml
java -jar build/libs/org.mri-1.*.jar --classpath-file "../Axon-trader/this.classpath" -s ../Axon-trader/ -m createuser -f dot > createuser.dot
diff createuser.dot regression/createuser.dot
