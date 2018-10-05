# Java実行環境フォント一覧ユーテリティ [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Javaで実行環境でサポートされている、すべてのフォントと、UIManagerが示すデフォルトフォントの関係を一覧表示する。

また、日本語グリフをもつフォントを判定する。(ひらがな、中点(・)、サロゲートペア、絵文字)

Java7以降のランタイムが必要です。

(Release ver1.0まではJava6でビルドしています)


使い方
------

起動すると、ただちにフォントの一覧を表示します。

サンプル用のテキストは起動した実行可能jarのある位置にある「sample.txt」というテキストがあれば、
そのテキストの内容を読み込みサンプル表示に使用します。


![screen capture 1](src/site/resources/images/screen-capture1.png?raw=true "screen capture1")

結果テーブルはファイルメニューよりファイルとして保存できます。


ビルド方法
----------------
[![Build Status](https://travis-ci.org/seraphy/JavaEnumFont.svg)](https://travis-ci.org/seraphy/JavaEnumFont)

プロジェクトはNetBeans8で作成されました。

ビルドにはMavenが必要です。

mvn clean package

で、targetディレクトリ上に実行可能jarが生成されます。



ライセンス
----------
Copyright &copy; 2015 seraphyware

Licensed under the [Apache License, Version 2.0][Apache]

[Apache]: http://www.apache.org/licenses/LICENSE-2.0
