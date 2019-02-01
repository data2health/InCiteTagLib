explain select * from (select domain,id,url from document,pattern2
where domain='icts.uiowa.edu'
and url~pattern
and visited is null
and (suffix is null or suffix not in (select suffix from jsoup.suffix))
) as foo where  not exists (select domain from pattern2 as bar where url~bar.exclude and url~bar.pattern);

explain select domain,id,url from document,pattern
where domain='icts.uiowa.edu'
and did=14490
and url~pattern
and not exists (select domain from pattern3 as bar where document.url~bar.exclude);

explain select domain,id,url from document,pattern
where domain='icts.uiowa.edu'
and did=14490
and url~pattern
and not exists ( select exclude from (select distinct exclude from pattern2 where domain ='icts.uiowa.edu') as foo where url~exclude);

    select distinct exclude from pattern2 where domain ='icts.uiowa.edu';

    ---------
    
create table staging(url text primary key, id int);

truncate staging;
insert into staging select url,id from document,pattern
where url~pattern and indexed  is null and domain='icts.uiowa.edu' and (suffix is null or suffix not in (select suffix from jsoup.suffix));

select url from staging
where not exists (select pattern from host_disallow where domain='icts.uiowa.edi' and url~pattern)
and not exists(select pattern from host_disallow_local where domain='icts.uiowa.edu' and url~pattern);

select id,url from staging
where not exists (select pattern from host_disallow where domain='icts.uiowa.edi' and url~pattern)
and not exists(select pattern from host_disallow_local where (domain='*' or domain='icts.uiowa.edu') and url~pattern) order by 2;

======= CD2H Proposals Phase2 =======

create view member as 
select 30 as id,id as pid from matrix natural join person_id natural join cd2h_people where trim(data) ='Lead'
union
select 25 as id,id as pid from matrix natural join person_id natural join cd2h_people where trim(data_1) ='Lead'
union
select 8 as id,id as pid from matrix natural join person_id natural join cd2h_people where trim(data_2) ='Lead'
union
select 26 as id,id as pid from matrix natural join person_id natural join cd2h_people where trim(data_3) ='Lead'
union
select 20 as id,id as pid from matrix natural join person_id natural join cd2h_people where trim(data_4) ='Lead'
union
select 13 as id,id as pid from matrix natural join person_id natural join cd2h_people where trim(data_5) ='Lead'
union
select 10 as id,id as pid from matrix natural join person_id natural join cd2h_people where trim(data_6) ='Lead'
union
select 7 as id,id as pid from matrix natural join person_id natural join cd2h_people where trim(data_7) ='Lead'
;

SELECT a.updated_title,
    btrim(b.person) AS btrim
   FROM source a,
    ( SELECT source.updated_title,
            regexp_split_to_table(source.project_team_members_affiliated_with_the_cd2h, '[,\n]'::text) AS person
           FROM source) b
  WHERE a.updated_title = b.updated_title;

  SELECT foo.id,
    person.id AS pid
   FROM ( SELECT proposal_id.id,
            source_person.btrim
           FROM proposal_id,
            source_person
          WHERE proposal_id.proposal = source_person.updated_title) foo,
    person
  WHERE foo.btrim ~ (person.last || '$'::text);
  
   SELECT proposal_id.id,
    person.id AS pid
   FROM ( SELECT foo.updated_title,
            foo.person
           FROM ( SELECT a.updated_title,
                    btrim(b.person) AS person
                   FROM source a,
                    ( SELECT source.updated_title,
                            regexp_split_to_table(regexp_replace(source.point_person, ' and '::text, ' & '::text), '[,\n&:()]'::text) AS person
                           FROM source) b
                  WHERE a.updated_title = b.updated_title) foo
          WHERE foo.person <> 'primary'::text AND foo.person <> ''::text AND foo.person <> 'TBD'::text AND foo.person !~ 'COMBINED'::text AND foo.person !~ 'merged'::text) bar,
    proposal_id,
    person
  WHERE bar.updated_title = proposal_id.proposal AND bar.person ~ (person.last || '$'::text)
  ORDER BY proposal_id.id;
