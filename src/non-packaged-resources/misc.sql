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
