CREATE TABLE incite.scan (
       id INT NOT NULL
     , source TEXT
     , size INT
     , pages INT
     , title TEXT
     , PRIMARY KEY (id)
);

CREATE TABLE jsoup.institution (
       did SERIAL NOT NULL
     , domain TEXT CONSTRAINT UQ_institution_1 UNIQUE
     , name TEXT
     , PRIMARY KEY (did)
);

CREATE TABLE incite.reference (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , type TEXT
     , tag TEXT
     , title TEXT
     , context TEXT
     , year INT
     , pub_date TEXT
     , pub_date_modifier TEXT
     , volume TEXT
     , issue TEXT
     , start_page TEXT
     , end_page TEXT
     , location TEXT
     , remainder TEXT
     , url TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_reference_1 FOREIGN KEY (id)
                  REFERENCES incite.scan (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE web.document (
       id SERIAL NOT NULL
     , url TEXT CONSTRAINT UQ_document_1 UNIQUE
     , title TEXT
     , length INT
     , modified TIMESTAMP
     , indexed TIMESTAMP
     , visited TIMESTAMP
     , did INTEGER NOT NULL
     , suffix TEXT
     , PRIMARY KEY (id)
     , CONSTRAINT FK_document_1 FOREIGN KEY (did)
                  REFERENCES web.institution (did) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE incite.author (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , first_name TEXT
     , middle_name TEXT
     , surname TEXT
     , title TEXT
     , suffix TEXT
     , department TEXT
     , institution TEXT
     , address TEXT
     , email TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_TABLE_2_1 FOREIGN KEY (id)
                  REFERENCES incite.scan (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE incite.abstract (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , abstract TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_abstract_1 FOREIGN KEY (id)
                  REFERENCES incite.scan (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE incite.keyword (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , keyword TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_keyword_1 FOREIGN KEY (id)
                  REFERENCES incite.scan (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE incite.acm_general_term (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , term TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_acm_general_term_1 FOREIGN KEY (id)
                  REFERENCES incite.scan (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE incite.acm_category (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , category TEXT
     , label TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_acm_category_1 FOREIGN KEY (id)
                  REFERENCES incite.scan (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE incite.ams_category (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , category TEXT
     , label TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_ams_category_1 FOREIGN KEY (id)
                  REFERENCES incite.scan (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE incite.pacs_category (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , category TEXT
     , label TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_pacs_category_1 FOREIGN KEY (id)
                  REFERENCES incite.scan (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE incite.jel_category (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , category TEXT
     , label TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_jel_category_1 FOREIGN KEY (id)
                  REFERENCES incite.scan (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE incite.reference_author (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , authnum INT NOT NULL
     , first_name TEXT
     , middle_name TEXT
     , surname TEXT
     , title TEXT
     , suffix TEXT
     , PRIMARY KEY (id, seqnum, authnum)
     , CONSTRAINT FK_reference_author_1 FOREIGN KEY (id, seqnum)
                  REFERENCES incite.reference (id, seqnum) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE web.link (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , url TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_link_1 FOREIGN KEY (id)
                  REFERENCES web.document (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE web.token (
       id INT NOT NULL
     , token TEXT NOT NULL
     , count INT
     , frequency DOUBLE PRECISION
     , PRIMARY KEY (id, token)
     , CONSTRAINT FK_token_1 FOREIGN KEY (id)
                  REFERENCES web.document (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE web.pmid (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , pmid INT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_pmid_1 FOREIGN KEY (id)
                  REFERENCES web.document (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE web.doi (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , doi TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_doi_1 FOREIGN KEY (id)
                  REFERENCES web.document (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE web.hyperlink (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , target INT
     , anchor TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_TABLE_13_1 FOREIGN KEY (id)
                  REFERENCES web.document (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE web.award (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , award TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_award_1 FOREIGN KEY (id)
                  REFERENCES web.document (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE web.sentence (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , sentence TEXT
     , tokens TEXT
     , PRIMARY KEY (id, seqnum)
     , CONSTRAINT FK_sentence_1 FOREIGN KEY (id)
                  REFERENCES web.document (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE incite.affiliation (
       id INT NOT NULL
     , seqnum INT NOT NULL
     , affnum INT NOT NULL
     , department TEXT
     , institution TEXT
     , address TEXT
     , email TEXT
     , PRIMARY KEY (id, seqnum, affnum)
     , CONSTRAINT FK_affiliation_1 FOREIGN KEY (id, seqnum)
                  REFERENCES incite.author (id, seqnum) ON DELETE CASCADE ON UPDATE CASCADE
);

