# RUN ME
.\gradlew.bat run

# Provera pre startovanja
## Provera da li Java radi:
java -version
## Provera da li Gradle radi:
.\gradlew.bat --version


# Izvrsi testove
.\gradlew.bat testDao 
* testira DAO sloj i pristup tabelama

.\gradlew.bat analyzeSchema — analizira šemu i tabele u bazama
.\gradlew.bat testObracun — testira obračun zarada

.\gradlew.bat test --info


