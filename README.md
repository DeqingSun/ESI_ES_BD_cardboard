# ESI_ES_BD_cardboard

### How to turn on device automatically when charger is plugged

Tested on Locked Nexus6 (No need to unlock or root to do this)

Enter fastboot and use

*fastboot oem off-mode-charge*

to test if your device support this command and it's default state.

The following command can turn off charge mode, and your phone will turn on when charger is connected.

*fastboot oem off-mode-charge 0*

ref: http://forum.xda-developers.com/showthread.php?t=1815131&page=4

###Handy cmds

unlock screen

/Applications/adt-bundle-mac-x86_64-20140702/sdk/platform-tools/adb shell input keyevent 82
