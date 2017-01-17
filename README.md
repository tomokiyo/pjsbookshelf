# ピッツバーグ補習授業校 図書管理システム

## 必要なツール・ライブラリ
* Git - MacでHomebrewを使う場合、`brew install git`を実行。
* Ant - MacでHomebrewを使う場合、`brew install ant`を実行。
* GWT (Google Web Toolkit) - [2.6.1](http://www.gwtproject.org/versions.html)で動作確認済み。より最新のバージョンでは変更が必要かもしれません。デフォルトでは `~/sdk` にSDKを置き、`~/sdk/gwt-2.6.1/gwt-servlet.jar`が存在することを確認してください。
* Apache Derby (`derby.jar`は既にlibディレクトリに含んでいるが、コマンドラインでSQLを実行したいときなど）

## コードの入手
```
$ mkdir -p ~/pjs/LibraryManager
$ cd ~/pjs/LibraryManager
$ git clone git@github.com:tomokiyo/pjsbookshelf.git system
$ cd system
```

## コンパイル
```
$ ant war
```
で `LibraryManager.war` を生成します。

## データの準備
別途入手したDerbyデータベースバイナリを設置します。個人情報が含まれるので管理に注意してください。
```
$ cp /path/to/PJS-DB ~/pjs/LibraryManager
```

## 実行
コンテナはServletが動くHTTPサーバーなら何でも良いですが、簡単なのはJettyをjetty runnerから起動するのが最も手軽。

```
$ ./dist/start-server.sh
```
を実行して `localhost:8080` にブラウザでアクセスしてください。
