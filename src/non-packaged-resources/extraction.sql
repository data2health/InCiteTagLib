select distinct
	disease.*,
	sentence,
	umls_id,
	umls_match_string,
	url,
	title
from
	extraction.disease,
	extraction.disease_match,
	extraction.disease_mention,
	extraction.sentence,
	jsoup.document
where disease.id=disease_match.id
  and disease.id=disease_mention.id
  and disease_mention.pmcid=sentence.id
  and disease_mention.segnum=sentence.seqnum
  and disease_mention.sentnum=sentence.setnum
  and sentence.id = document.id
;

select distinct
	person.*,
	sentence,
	url,
	document.title
from
	extraction.person,
	extraction.person_mention,
	extraction.sentence,
	jsoup.document
where person.id=person_mention.id
  and person_mention.pmcid=sentence.id
  and person_mention.segnum=sentence.seqnum
  and person_mention.sentnum=sentence.setnum
  and sentence.id = document.id
;

select distinct
	substance.*,
	sentence,
	url,
	document.title
from
	extraction.substance,
	extraction.substance_mention,
	extraction.sentence,
	jsoup.document
where substance.sid=substance_mention.sid
  and substance_mention.pmcid=sentence.id
  and substance_mention.segnum=sentence.seqnum
  and substance_mention.sentnum=sentence.setnum
  and sentence.id = document.id
;


-- sentence frequency suppresion

select * from
(select did,host,sentence,count(*) as sent_count from sentence natural join jsoup.document where did = 24907 group by 1,2) as foo
natural join
(select did,count as doc_count from jsoup.document_host where indexed is not null) as bar
;

select *,(sent_count * 1.0)/host_count as freq from
(select did,host,sentence,count(*) as sent_count from sentence natural join jsoup.document natural join jsoup.document_host where did = 24907 and url~('^'||host) group by 1,2,3) as foo
natural join
(select host,count as host_count from jsoup.document_host) as bar
;
