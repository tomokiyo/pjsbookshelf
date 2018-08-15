-- 貸出回数が少ない図書のリスト（過去5年間における、貸出回数が0～5回の図書のリスト）
--
-- 目的：図書室全体の蔵書整理を実施する際の参考のため。
-- 出力項目：蔵書番号, 題名, 著者, 出版社, 登録年, 貸出回数
-- 備考：例年、8/20頃までに必要。

CONNECT 'jdbc:derby:../PJS-DB/pjsLibraryDB';

-- 貸出回数0回の本
SELECT
  Book.id,
  Book.title,
  Book.authors,
  Book.publisher,
  YEAR(Book.register_date),
  0
FROM
  Book
WHERE
  discard_date IS NULL
  AND NOT EXISTS(
    SELECT
      1
    FROM
      CheckoutHistory
    WHERE
      Book.id = CheckoutHistory.book_id
    )
  ORDER BY
    Book.id ASC;

-- 貸出回数1〜5回の本
SELECT
  Book.id,
  Book.title,
  Book.authors,
  Book.publisher,
  YEAR(Book.register_date),
  Count(*)
FROM
  CheckoutHistory
INNER JOIN
  Book ON book_id = Book.id
WHERE
  discard_date IS NULL
  AND checkout_date > {fn TIMESTAMPADD(SQL_TSI_YEAR, -5, CURRENT_TIMESTAMP)}
GROUP BY
  Book.id,
  Book.title,
  Book.authors,
  Book.publisher,
  Book.register_date
HAVING
  Count(*) <= 5
ORDER BY
  Count(*) ASC;

DISCONNECT;
