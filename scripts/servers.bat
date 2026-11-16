@echo off
setlocal

REM ============================================================
REM  servers.bat  -  control Tomcat, Jetty and WildFly locally
REM
REM  Usage:   servers start   [all|tomcat|jetty|wildfly]
REM           servers stop    [all|tomcat|jetty|wildfly]
REM           servers status
REM           servers deploy  [all|gateway|analysis|cleaning]
REM  Default target is "all".
REM ============================================================

REM ===== PATHS =====
set SERVERS=C:\Dev\Uni\MASTER\TPJAD\T1_Servlets\servers
set TOMCAT_HOME=%SERVERS%\tomcat\apache-tomcat-10.1.61-windows-x64\apache-tomcat-10.1.61
set JETTY_HOME=%SERVERS%\jetty\jetty-home-12.0.40
set JETTY_BASE=%SERVERS%\jetty\jetty-base
set WILDFLY_HOME=%SERVERS%\wildfly\wildfly-41.0.1.Final
set PROJECT=C:\Dev\Uni\MASTER\TPJAD\T1_Servlets\csv-platform

REM ===== App configuration (read by the servlets via System.getenv) =====
set DATA_DIR=C:\Dev\Uni\MASTER\TPJAD\T1_Servlets\shared
set ANALYSIS_URL=http://localhost:8081/analysis
set CLEANING_URL=http://localhost:8082/cleaning

REM Needed by Tomcat's scripts
set CATALINA_HOME=%TOMCAT_HOME%

set ACTION=%1
set TARGET=%2
if "%ACTION%"=="" goto usage
if "%TARGET%"=="" set TARGET=all

if /i "%ACTION%"=="start"  goto start
if /i "%ACTION%"=="stop"   goto stop
if /i "%ACTION%"=="status" goto status
if /i "%ACTION%"=="deploy" goto deploy
goto usage

:usage
echo Usage: %~nx0 start^|stop [all^|tomcat^|jetty^|wildfly]
echo        %~nx0 status
echo        %~nx0 deploy [all^|gateway^|analysis^|cleaning]
goto :eof

REM ------------------------- START -------------------------
:start
if not exist "%DATA_DIR%\jobs" mkdir "%DATA_DIR%\jobs"
if /i "%TARGET%"=="all"     ( call :start_tomcat & call :start_jetty & call :start_wildfly & goto :eof )
if /i "%TARGET%"=="tomcat"  ( call :start_tomcat  & goto :eof )
if /i "%TARGET%"=="jetty"   ( call :start_jetty   & goto :eof )
if /i "%TARGET%"=="wildfly" ( call :start_wildfly & goto :eof )
goto usage

:start_tomcat
echo Starting Tomcat on 8080...
start "Tomcat (8080)" cmd /k "%TOMCAT_HOME%\bin\catalina.bat" run
goto :eof

:start_jetty
echo Starting Jetty on 8081...
start "Jetty (8081)" /D "%JETTY_BASE%" cmd /k java -jar "%JETTY_HOME%\start.jar"
goto :eof

:start_wildfly
echo Starting WildFly on 8082 (management 9992)...
start "WildFly (8082)" cmd /k ""%WILDFLY_HOME%\bin\standalone.bat" "-Djboss.socket.binding.port-offset=2""
goto :eof

REM ------------------------- STOP -------------------------
:stop
if /i "%TARGET%"=="all"     ( call :stop_tomcat & call :stop_jetty & call :stop_wildfly & goto :eof )
if /i "%TARGET%"=="tomcat"  ( call :stop_tomcat  & goto :eof )
if /i "%TARGET%"=="jetty"   ( call :stop_jetty   & goto :eof )
if /i "%TARGET%"=="wildfly" ( call :stop_wildfly & goto :eof )
goto usage

:stop_tomcat
echo Stopping Tomcat...
call "%TOMCAT_HOME%\bin\shutdown.bat"
goto :eof

:stop_jetty
echo Stopping Jetty (process listening on 8081)...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr LISTENING ^| findstr /R ":8081 "') do taskkill /PID %%p /F
goto :eof

:stop_wildfly
echo Stopping WildFly (management port 9992)...
call "%WILDFLY_HOME%\bin\jboss-cli.bat" --connect --controller=localhost:9992 command=:shutdown
goto :eof

REM ------------------------- STATUS -------------------------
:status
echo Listening on 8080 / 8081 / 8082:
netstat -ano | findstr LISTENING | findstr /R ":808[012] "
goto :eof

REM ------------------------- DEPLOY -------------------------
REM Copies the built WARs (mvn clean package) into each server's deploy folder.
:deploy
if /i "%TARGET%"=="all"      ( call :deploy_gateway & call :deploy_analysis & call :deploy_cleaning & goto :eof )
if /i "%TARGET%"=="gateway"  ( call :deploy_gateway  & goto :eof )
if /i "%TARGET%"=="analysis" ( call :deploy_analysis & goto :eof )
if /i "%TARGET%"=="cleaning" ( call :deploy_cleaning & goto :eof )
goto usage

:deploy_gateway
REM Tomcat unpacks the WAR into a folder; remove the old one so the new WAR is picked up
if exist "%TOMCAT_HOME%\webapps\gateway" rmdir /s /q "%TOMCAT_HOME%\webapps\gateway"
copy /y "%PROJECT%\gateway\target\gateway.war" "%TOMCAT_HOME%\webapps\"
goto :eof

:deploy_analysis
copy /y "%PROJECT%\analysis\target\analysis.war" "%JETTY_BASE%\webapps\"
goto :eof

:deploy_cleaning
copy /y "%PROJECT%\cleaning\target\cleaning.war" "%WILDFLY_HOME%\standalone\deployments\"
goto :eof