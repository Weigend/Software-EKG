@echo off
echo ***********************************************
echo starte Dienste 'RemoteRegistry' und 'Server'...
echo ***********************************************
sc config RemoteRegistry start= auto
net start RemoteRegistry
sc config lanmanserver start= auto
net start lanmanserver

echo ***********************
echo aktiviere Firewall...
echo ***********************
netsh firewall set opmode enable

echo ***********************
echo schalte Datei- und Druckerfreigabe frei ....
echo ***********************      
netsh firewall set service FILEANDPRINT ENABLE

echo *****************************************************************************
echo passe Berechtigungen an...
echo Erscheint eine Fehlermeldung, so ist das System bereits richtig konfiguriert.
echo *****************************************************************************
rem ntrights.exe is part of the Windows Server Resource Kit Tools and has to be on the PATH
ntrights -r SeDenyNetworkLogonRight -u Administrator

echo Instrumentierung abgeschlossen!