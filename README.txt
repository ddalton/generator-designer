java -jar target/odor-1.0-SNAPSHOT-jar-with-dependencies.jar -c "jdbc:h2:mem:db2;MVCC=TRUE;autocommit=off;USER=sa;PASSWORD=123" -r "org.h2.Driver" -s "/Users/i844711/Downloads/odor/schema.sql" -u "sa" -p "123" -d "//"

java -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -jar target/odor-1.0-SNAPSHOT-jar-with-dependencies.jar -c "jdbc:h2:mem:db2;MVCC=TRUE;autocommit=off;USER=sa;PASSWORD=123" -r "org.h2.Driver" -s "/Users/i844711/Downloads/odor/schema.sql" -u "sa" -p "123" -d "//"

