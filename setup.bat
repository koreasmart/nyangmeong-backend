@echo off
setlocal
for /f "tokens=2 delims=:" %%c in ('chcp') do set "OLDCP=%%c"
chcp 65001 >nul
rem Windows cmd·PowerShell에서 setup.sh를 Git Bash로 실행한다. 설치 내용은 모두 setup.sh에 있다.
rem cmd·PowerShell에서 bash를 치면 Git Bash 대신 WSL이 잡히므로 이 파일을 둔다.
rem 백엔드 저장소의 setup.bat와 내용이 같다. 고칠 때 둘 다 고친다.

rem Git Bash 위치: GIT_BASH 환경변수 → 기본 설치 위치 → git.exe 위치(Git\cmd 또는 Git\mingw64\bin) 순서로 찾는다.
rem Scoop처럼 git.exe가 다른 폴더에 있으면 GIT_BASH에 bash.exe 경로를 넣고 실행한다
set "GITBASH="
if defined GIT_BASH if exist "%GIT_BASH%" set "GITBASH=%GIT_BASH%"
if not defined GITBASH if exist "%ProgramFiles%\Git\bin\bash.exe" set "GITBASH=%ProgramFiles%\Git\bin\bash.exe"
if not defined GITBASH for /f "delims=" %%g in ('where git 2^>nul') do (
  if not defined GITBASH if exist "%%~dpg..\bin\bash.exe" set "GITBASH=%%~dpg..\bin\bash.exe"
  if not defined GITBASH if exist "%%~dpg..\..\bin\bash.exe" set "GITBASH=%%~dpg..\..\bin\bash.exe"
)
if not defined GITBASH (
  echo.
  echo [중단] Git Bash를 찾지 못했습니다. Git for Windows를 설치하거나, 설치돼 있으면 GIT_BASH 환경변수에 bash.exe 경로를 넣고 다시 실행해 주세요.
  set "RC=1"
  goto end
)

"%GITBASH%" "%~dp0setup.sh" %*
set "RC=%ERRORLEVEL%"

:end
rem 탐색기 더블클릭이면 결과를 읽을 수 있게 성공·실패와 관계없이 창이 바로 닫히지 않게 멈춘다. cmd·PowerShell에서 실행하면 창이 남으므로 멈추지 않는다.
rem 더블클릭은 cmdcmdline이 ...setup.bat" " 로 끝나 끝에서 두 번째 글자가 공백이다(PowerShell은 "" 로 끝난다).
rem 경로에 & 같은 글자가 있어도 깨지지 않도록 지연 확장으로 꺼낸다
setlocal EnableDelayedExpansion
if "!cmdcmdline:~-2,1!"==" " pause
endlocal
chcp %OLDCP% >nul
exit /b %RC%
