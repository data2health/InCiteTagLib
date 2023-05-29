create view display.organization_staging as
select
	grid_id,
	name,
	city,
	state,
	country,
	latitude,
	longitude,
	sum(count) as count
from extraction.organization, extraction.organization_mention,grid.institute,grid.address
where organization.id = organization_mention.id
  and organization.grid_id = institute.id
  and institute.id = address.id
  and address.seqnum = 1
group by 1,2,3,4,5,6,7
;

create view display.organization as
select
	organization_staging.*,
	coalesce(type, 'unspecified') as type
from display.organization_staging left outer join grid.type on(grid_id = id)
;

create view display.substance as
select
	sid,
	substance,
	mesh,
	pharm,
	count
from
	(select sid,substance,sum(count) as count from extraction.substance
	 natural join extraction.substance_mention group by 1) as foo
natural join
	(select sid,mesh from pubchem_substance.sid_mesh) as bar
natural join
	pubchem_substance.mesh_pharm
;

select distinct
	cui,
	str,
	ancestor_cui,
	ancestor_str,
	count
from 
	(select organism,umls_id as cui,sum(count) as count
	 from extraction.organism natural join extraction.organism_match natural join extraction.organism_mention
	 group by 1,2) as bar
natural left outer join
	(select cui,str,ancestor_cui,ancestor_str from umls_local.hierarchy_full) as foo
order by 3 desc,1
;


create materialized view hierarchy_filtered3 as
select distinct
	foo.cui,
	foo.str,
	foo.ancestor_cui,
	foo.ancestor_str,
	bar.ancestor_cui as root_cui,
	bar.ancestor_str as root_str
from hierarchy_full as foo, hierarchy_full bar
where foo.depth=3
  and foo.ancestor_cui = bar.cui
  and exists (select * from umls.mrconso where foo.ancestor_aui=mrconso.aui and lat='ENG')
  and bar.depth=2
;

create index hfcui3 on hierarchy_filtered3(cui);
create index hfacui3 on hierarchy_filtered3(ancestor_cui);

create materialized view hierarchy_filtered4 as
select distinct
	foo.cui,
	foo.str,
	foo.ancestor_cui,
	foo.ancestor_str,
	bar.ancestor_cui as root_cui,
	bar.ancestor_str as root_str
from hierarchy_full as foo, hierarchy_full bar
where foo.depth=4
  and foo.ancestor_cui = bar.cui
  and exists (select * from umls.mrconso where foo.ancestor_aui=mrconso.aui and lat='ENG')
  and bar.depth=2
;

create index hfcui4 on hierarchy_filtered4(cui);
create index hfacui4 on hierarchy_filtered4(ancestor_cui);

create materialized view hierarchy_filtered5 as
select distinct
	foo.cui,
	foo.str,
	foo.ancestor_cui,
	foo.ancestor_str,
	bar.ancestor_cui as root_cui,
	bar.ancestor_str as root_str
from hierarchy_full as foo, hierarchy_full bar
where foo.depth=5
  and foo.ancestor_cui = bar.cui
  and exists (select * from umls.mrconso where foo.ancestor_aui=mrconso.aui and lat='ENG')
  and bar.depth=2
;

create index hfcui5 on hierarchy_filtered5(cui);
create index hfacui5 on hierarchy_filtered5(ancestor_cui);

select * from
	(select umls_id as cui,sum(count) as count from extraction.organism_match natural join extraction.organism_mention group by 1) as foo
natural join
	(select distinct cui,str,root_str from hierarchy_filtered3) as bar
;

// source frequency counts
select ancestor_cui,ancestor_str,sum(count) from
(select umls_id as cui,sum(count) as count from extraction.organism_match natural join extraction.organism_mention group by 1) as foo
natural join
(select distinct cui,ancestor_cui,ancestor_str from umls_local.hierarchy_full
where depth = 2
  and exists (select * from umls.mrconso where hierarchy_full.ancestor_aui=mrconso.aui and lat='ENG')
) as bar
group by 1,2
order by 3 desc;

// source frequency counts
select ancestor_cui,ancestor_str,root_cui,root_str,sum(count) from
	(select umls_id as cui,sum(count) as count from extraction.organism_match natural join extraction.organism_mention group by 1) as foo
natural join
	(select distinct cui,ancestor_cui,ancestor_str from umls_local.hierarchy_full
	 where depth = 3
	   and exists (select * from umls.mrconso where hierarchy_full.ancestor_aui=mrconso.aui and lat='ENG')
	) as bar
natural join
	(select distinct cui as ancestor_cui,ancestor_cui as root_cui,ancestor_str as root_str from umls_local.hierarchy_full
	 where depth = 2
	   and exists (select * from umls.mrconso where hierarchy_full.ancestor_aui=mrconso.aui and lat='ENG')
	) as bar2
group by 1,2,3,4
order by 5 desc
;

