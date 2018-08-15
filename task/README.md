# SQLタスク

## 必要なツール・ライブラリ
Apache Derby. 環境変数 `$DERBY_HOME`, `$PATH` の設定を行ってください。以下は一例です。

```
export DERBY_HOME=/Users/PJS/sdk/db-derby-10.5.3.0-bin
export PATH="$DERBY_HOME/bin:$PATH"
```

## 実行方法
このディレクトリ内の `.sql` ファイルはDerbyのコマンドラインツール `ij` で以下のように実行可能です。
```
ij < task/list_unpopular_books.sql
```

実行結果は必要に応じて整形し、ファイルに保存してください。
```
ij < task/list_unpopular_books.sql | perl -p -e 's/\s+\|/<>/g; s/\t/ /g; s/<>/\t/g;' > out.tsv
```
