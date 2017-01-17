# ピッツバーグ補習授業校 図書管理システム
(Automatically exported from code.google.com/p/pjsbookshelf)

## 必要なライブラリ
*  GWT (Google Web Toolkit) - 2.6.1で動作確認済み。より最新のバージョンでは変更が必要かもしれません。
*  Apache Derby (derby.jarは既にlibディレクトリに含んでいるが、コマンドラインでSQLを実行したいときなど）

## コンパイル
$ ant war
で LibraryManager.war　を生成します。

## 実行
コンテナはServletが動くHTTPサーバーなら何でも良いですが、簡単なのはJettyをjetty runnerから起動するのが最も手軽。
$ ./dist/start-server.sh
を実行して localhost:8080 にブラウザでアクセスしてください。
