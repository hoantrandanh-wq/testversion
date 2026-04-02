!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "nsDialogs.nsh"
!include "FileFunc.nsh"

!ifndef APP_VERSION
!define APP_VERSION "dev"
!endif

!ifndef INSTALLER_SOURCE_DIR
!define INSTALLER_SOURCE_DIR "dist\\BDMA"
!endif

!ifndef INSTALLER_OUTPUT_DIR
!define INSTALLER_OUTPUT_DIR "."
!endif

!ifndef INSTALLER_ICON
!define INSTALLER_ICON "..\\image\\logo.ico"
!endif

Name "BDMA"
OutFile "${INSTALLER_OUTPUT_DIR}\\BDMA-${APP_VERSION}-Setup.exe"
Icon "${INSTALLER_ICON}"
InstallDir "$PROGRAMFILES64\BDMA"
InstallDirRegKey HKLM "Software\BDMA" "InstallDir"
RequestExecutionLevel admin
Unicode True

!define MUI_ABORTWARNING

Var Dialog
Var RadioInstall
Var RadioUninstall
Var RadioUninstallDelete
Var UserChoice
Var IsInstalled

; ── Trang chọn action (chỉ hiện khi đã cài) ─────────────────────
Page custom ShowActionDialog ShowActionDialogLeave
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_LANGUAGE "English"

; ── Detect đã cài chưa ──────────────────────────────────────────
Function .onInit
  ReadRegStr $IsInstalled HKLM "Software\BDMA" "InstallDir"
  ${If} $IsInstalled != ""
    StrCpy $IsInstalled "1"
  ${Else}
    StrCpy $IsInstalled "0"
    StrCpy $UserChoice "1"
  ${EndIf}
FunctionEnd

; ── Dialog 3 lựa chọn ───────────────────────────────────────────
Function ShowActionDialog
  ${If} $IsInstalled == "0"
    Abort ; Chưa cài → bỏ qua trang này, install thẳng
  ${EndIf}

  !insertmacro MUI_HEADER_TEXT "BDMA đã được cài đặt" "Vui lòng chọn hành động"

  nsDialogs::Create 1018
  Pop $Dialog

  ${NSD_CreateLabel} 0 0 100% 24u "BDMA đã được cài đặt trên máy của bạn. Bạn muốn làm gì?"

  ${NSD_CreateRadioButton} 10u 34u 100% 14u "Cài đặt lại / Cập nhật"
  Pop $RadioInstall
  ${NSD_SetState} $RadioInstall ${BST_CHECKED}

  ${NSD_CreateRadioButton} 10u 52u 100% 14u "Gỡ cài đặt"
  Pop $RadioUninstall

  ${NSD_CreateRadioButton} 10u 70u 100% 14u "Gỡ cài đặt và xóa toàn bộ dữ liệu"
  Pop $RadioUninstallDelete

  nsDialogs::Show
FunctionEnd

Function ShowActionDialogLeave
  ${NSD_GetState} $RadioInstall $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $UserChoice "1"
    Goto done
  ${EndIf}

  ${NSD_GetState} $RadioUninstall $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $UserChoice "2"
    Goto done
  ${EndIf}

  StrCpy $UserChoice "3"
  done:
FunctionEnd

; ── Section chính ───────────────────────────────────────────────
Section "Main" SecMain
  ${If} $UserChoice == "2"
    Call DoUninstall
    Quit
  ${EndIf}

  ${If} $UserChoice == "3"
    Call DoUninstall
    Call DeleteData
    Quit
  ${EndIf}

  ; ── Install / Update ────────────────────────────────────────
  SetOutPath "$INSTDIR"
  File /r "${INSTALLER_SOURCE_DIR}\*.*"

  ; Copy file setup vào thư mục cài đặt để làm uninstaller
  CopyFiles "$EXEPATH" "$INSTDIR\BDMA-Setup.exe"

  ; Ghi registry
  WriteRegStr HKLM "Software\BDMA" "InstallDir" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\BDMA" \
    "DisplayName" "BDMA"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\BDMA" \
    "UninstallString" '"$INSTDIR\BDMA-Setup.exe"'
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\BDMA" \
    "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\BDMA" \
    "DisplayIcon" "$INSTDIR\BDMA.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\BDMA" \
    "Publisher" "DVID"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\BDMA" \
    "DisplayVersion" "${APP_VERSION}"
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\BDMA" \
    "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\BDMA" \
    "NoRepair" 1

  ${GetSize} "$INSTDIR" "/S=0K" $0 $1 $2
  IntFmt $0 "0x%08X" $0
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\BDMA" \
    "EstimatedSize" $0

  ; Shortcut
  CreateShortcut "$DESKTOP\BDMA.lnk" "$INSTDIR\BDMA.exe"
  CreateDirectory "$SMPROGRAMS\BDMA"
  CreateShortcut "$SMPROGRAMS\BDMA\BDMA.lnk" "$INSTDIR\BDMA.exe"
  CreateShortcut "$SMPROGRAMS\BDMA\Gỡ cài đặt.lnk" "$INSTDIR\BDMA-Setup.exe"

  MessageBox MB_OK "Cài đặt BDMA thành công!"
SectionEnd

; ── Uninstall ───────────────────────────────────────────────────
Function DoUninstall
  RMDir /r "$INSTDIR\app"
  RMDir /r "$INSTDIR\runtime"
  Delete "$INSTDIR\BDMA.exe"
  Delete "$INSTDIR\BDMA-Setup.exe"
  RMDir "$INSTDIR"

  Delete "$DESKTOP\BDMA.lnk"
  Delete "$SMPROGRAMS\BDMA\BDMA.lnk"
  Delete "$SMPROGRAMS\BDMA\Gỡ cài đặt.lnk"
  RMDir "$SMPROGRAMS\BDMA"

  DeleteRegKey HKLM "Software\BDMA"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\BDMA"

  MessageBox MB_OK "Gỡ cài đặt BDMA thành công!"
FunctionEnd

; ── Xóa data ────────────────────────────────────────────────────
Function DeleteData
  RMDir /r "$LOCALAPPDATA\bdma"
FunctionEnd