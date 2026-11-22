!macro customInit
  ; Terminate any previous running instances of AetherLink or VEXYRON
  nsExec::Exec 'taskkill /F /IM aether-link.exe /IM aetherlink.exe /IM desktop.exe /IM vexyron.exe /T'
  ; Remove legacy AetherLink program directory so only one application exists
  RMDir /r "$LOCALAPPDATA\Programs\AetherLink"
  RMDir /r "$LOCALAPPDATA\AetherLink"
  Delete "$SMPROGRAMS\AetherLink.lnk"
  Delete "$DESKTOP\AetherLink.lnk"
!macroend
