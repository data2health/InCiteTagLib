--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.3
-- Dumped by pg_dump version 9.6.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pubmed_central_ack_stanford; Type: SCHEMA; Schema: -; Owner: eichmann
--

CREATE SCHEMA entity;


ALTER SCHEMA pubmed_central_ack_stanford OWNER TO eichmann;

SET search_path = entity, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: award_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE award_mention (
    award_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE award_mention OWNER TO eichmann;

--
-- Name: collaboration_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE collaboration_mention (
    collaboration_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE collaboration_mention OWNER TO eichmann;

--
-- Name: discipline_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE discipline_mention (
    discipline_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE discipline_mention OWNER TO eichmann;

--
-- Name: disease_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE disease_mention (
    disease_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE disease_mention OWNER TO eichmann;

--
-- Name: event_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE event_mention (
    event_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE event_mention OWNER TO eichmann;

--
-- Name: funding_agency_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE funding_agency_mention (
    funding_agency_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE funding_agency_mention OWNER TO eichmann;

--
-- Name: location_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE location_mention (
    location_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE location_mention OWNER TO eichmann;

--
-- Name: organic_chemical_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE organic_chemical_mention (
    organic_chemical_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE organic_chemical_mention OWNER TO eichmann;

--
-- Name: organization_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE organization_mention (
    organization_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE organization_mention OWNER TO eichmann;

--
-- Name: person_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE person_mention (
    person_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE person_mention OWNER TO eichmann;

--
-- Name: project_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE project_mention (
    project_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE project_mention OWNER TO eichmann;

--
-- Name: publication_component_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE publication_component_mention (
    publication_component_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE publication_component_mention OWNER TO eichmann;

--
-- Name: resource_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE resource_mention (
    resource_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE resource_mention OWNER TO eichmann;

--
-- Name: support_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE support_mention (
    support_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE support_mention OWNER TO eichmann;

--
-- Name: technique_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE technique_mention (
    technique_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE technique_mention OWNER TO eichmann;

--
-- Name: ack_by_distinct_entity; Type: MATERIALIZED VIEW; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE MATERIALIZED VIEW ack_by_distinct_entity AS
 SELECT foo.id,
    count(*) AS count
   FROM ( SELECT DISTINCT award_mention.id,
            'award_mention'::text
           FROM award_mention
        UNION
         SELECT DISTINCT collaboration_mention.id,
            'collaboration_mention'::text
           FROM collaboration_mention
        UNION
         SELECT DISTINCT discipline_mention.id,
            'discipline_mention'::text
           FROM discipline_mention
        UNION
         SELECT DISTINCT disease_mention.id,
            'disease_mention'::text
           FROM disease_mention
        UNION
         SELECT DISTINCT event_mention.id,
            'event_mention'::text
           FROM event_mention
        UNION
         SELECT DISTINCT funding_agency_mention.id,
            'funding_agency_mention'::text
           FROM funding_agency_mention
        UNION
         SELECT DISTINCT location_mention.id,
            'location_mention'::text
           FROM location_mention
        UNION
         SELECT DISTINCT organic_chemical_mention.id,
            'organic_chemical_mention'::text
           FROM organic_chemical_mention
        UNION
         SELECT DISTINCT organization_mention.id,
            'organization_mention'::text
           FROM organization_mention
        UNION
         SELECT DISTINCT organization_mention.id,
            'organization_mention'::text
           FROM organization_mention
        UNION
         SELECT DISTINCT person_mention.id,
            'person_mention'::text
           FROM person_mention
        UNION
         SELECT DISTINCT project_mention.id,
            'project_mention'::text
           FROM project_mention
        UNION
         SELECT DISTINCT publication_component_mention.id,
            'publication_component_mention'::text
           FROM publication_component_mention
        UNION
         SELECT DISTINCT resource_mention.id,
            'resource_mention'::text
           FROM resource_mention
        UNION
         SELECT DISTINCT support_mention.id,
            'support_mention'::text
           FROM support_mention
        UNION
         SELECT DISTINCT technique_mention.id,
            'technique_mention'::text
           FROM technique_mention) foo
  GROUP BY foo.id
  ORDER BY (count(*)) DESC
  WITH NO DATA;


ALTER TABLE ack_by_distinct_entity OWNER TO eichmann;

--
-- Name: organism_mention; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE organism_mention (
    organism_id integer NOT NULL,
    id integer NOT NULL
);


ALTER TABLE organism_mention OWNER TO eichmann;

--
-- Name: ack_by_total_entity; Type: MATERIALIZED VIEW; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE MATERIALIZED VIEW ack_by_total_entity AS
 SELECT foo.id,
    sum(foo.count) AS sum
   FROM ( SELECT award_mention.id,
            count(*) AS count
           FROM award_mention
          GROUP BY award_mention.id
        UNION
         SELECT collaboration_mention.id,
            count(*) AS count
           FROM collaboration_mention
          GROUP BY collaboration_mention.id
        UNION
         SELECT discipline_mention.id,
            count(*) AS count
           FROM discipline_mention
          GROUP BY discipline_mention.id
        UNION
         SELECT disease_mention.id,
            count(*) AS count
           FROM disease_mention
          GROUP BY disease_mention.id
        UNION
         SELECT event_mention.id,
            count(*) AS count
           FROM event_mention
          GROUP BY event_mention.id
        UNION
         SELECT funding_agency_mention.id,
            count(*) AS count
           FROM funding_agency_mention
          GROUP BY funding_agency_mention.id
        UNION
         SELECT location_mention.id,
            count(*) AS count
           FROM location_mention
          GROUP BY location_mention.id
        UNION
         SELECT organic_chemical_mention.id,
            count(*) AS count
           FROM organic_chemical_mention
          GROUP BY organic_chemical_mention.id
        UNION
         SELECT organism_mention.id,
            count(*) AS count
           FROM organism_mention
          GROUP BY organism_mention.id
        UNION
         SELECT organization_mention.id,
            count(*) AS count
           FROM organization_mention
          GROUP BY organization_mention.id
        UNION
         SELECT person_mention.id,
            count(*) AS count
           FROM person_mention
          GROUP BY person_mention.id
        UNION
         SELECT project_mention.id,
            count(*) AS count
           FROM project_mention
          GROUP BY project_mention.id
        UNION
         SELECT publication_component_mention.id,
            count(*) AS count
           FROM publication_component_mention
          GROUP BY publication_component_mention.id
        UNION
         SELECT resource_mention.id,
            count(*) AS count
           FROM resource_mention
          GROUP BY resource_mention.id
        UNION
         SELECT support_mention.id,
            count(*) AS count
           FROM support_mention
          GROUP BY support_mention.id
        UNION
         SELECT technique_mention.id,
            count(*) AS count
           FROM technique_mention
          GROUP BY technique_mention.id) foo
  GROUP BY foo.id
  ORDER BY (sum(foo.count)) DESC
  WITH NO DATA;


ALTER TABLE ack_by_total_entity OWNER TO eichmann;

--
-- Name: ack_fragment; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE ack_fragment (
    id integer,
    seqnum integer,
    sentnum integer,
    node text,
    fragment text
);


ALTER TABLE ack_fragment OWNER TO eichmann;

--
-- Name: ack_parse; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE ack_parse (
    id integer,
    seqnum integer,
    sentnum integer,
    parsenum integer,
    parse text
);


ALTER TABLE ack_parse OWNER TO eichmann;

--
-- Name: ack_sentence; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE ack_sentence (
    id integer,
    seqnum integer,
    sentnum integer,
    sentence text,
    tokens text
);


ALTER TABLE ack_sentence OWNER TO eichmann;

--
-- Name: affiliation; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE affiliation (
    id integer,
    person_id integer,
    organization_id integer
);


ALTER TABLE affiliation OWNER TO eichmann;

--
-- Name: award; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE award (
    id integer NOT NULL,
    award text,
    agency text
);


ALTER TABLE award OWNER TO eichmann;

--
-- Name: award_backup; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE award_backup (
    id integer,
    award text,
    agency text
);


ALTER TABLE award_backup OWNER TO eichmann;

--
-- Name: award_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE award_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE award_id_seq OWNER TO eichmann;

--
-- Name: award_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE award_id_seq OWNED BY award.id;


--
-- Name: awardee; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE awardee (
    id integer NOT NULL,
    award_id integer NOT NULL,
    person_id integer NOT NULL
);


ALTER TABLE awardee OWNER TO eichmann;

--
-- Name: clinical_trial; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE clinical_trial (
    id integer,
    prefix text,
    id text
);


ALTER TABLE clinical_trial OWNER TO eichmann;

--
-- Name: collaborant; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE collaborant (
    id integer NOT NULL,
    organization_id integer NOT NULL,
    collaboration_id integer NOT NULL
);


ALTER TABLE collaborant OWNER TO eichmann;

--
-- Name: collaboration; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE collaboration (
    id integer NOT NULL,
    collaboration text
);


ALTER TABLE collaboration OWNER TO eichmann;

--
-- Name: collaboration_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE collaboration_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE collaboration_id_seq OWNER TO eichmann;

--
-- Name: collaboration_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE collaboration_id_seq OWNED BY collaboration.id;


--
-- Name: collaborator; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE collaborator (
    id integer NOT NULL,
    collaboration_id integer NOT NULL,
    person_id integer NOT NULL
);


ALTER TABLE collaborator OWNER TO eichmann;

--
-- Name: discipline; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE discipline (
    id integer NOT NULL,
    discipline text
);


ALTER TABLE discipline OWNER TO eichmann;

--
-- Name: discipline_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE discipline_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE discipline_id_seq OWNER TO eichmann;

--
-- Name: discipline_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE discipline_id_seq OWNED BY discipline.id;


--
-- Name: disease; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE disease (
    id integer NOT NULL,
    disease text,
    umls_id text,
    umls_match_string text
);


ALTER TABLE disease OWNER TO eichmann;

--
-- Name: disease_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE disease_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE disease_id_seq OWNER TO eichmann;

--
-- Name: disease_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE disease_id_seq OWNED BY disease.id;


--
-- Name: event; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE event (
    id integer NOT NULL,
    event text
);


ALTER TABLE event OWNER TO eichmann;

--
-- Name: event_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE event_id_seq OWNER TO eichmann;

--
-- Name: event_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE event_id_seq OWNED BY event.id;


--
-- Name: funder; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE funder (
    id integer NOT NULL,
    organization_id integer NOT NULL,
    award_id integer NOT NULL
);


ALTER TABLE funder OWNER TO eichmann;

--
-- Name: funding_agency; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE funding_agency (
    id integer NOT NULL,
    funding_agency text
);


ALTER TABLE funding_agency OWNER TO eichmann;

--
-- Name: investigator; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE investigator (
    id integer NOT NULL,
    person_id integer NOT NULL,
    organization_id integer NOT NULL
);


ALTER TABLE investigator OWNER TO eichmann;

--
-- Name: location; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE location (
    id integer NOT NULL,
    location text,
    geonames_id integer,
    geonames_match_string text
);


ALTER TABLE location OWNER TO eichmann;

--
-- Name: organic_chemical; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE organic_chemical (
    id integer NOT NULL,
    organic_chemical text,
    umls_id text,
    umls_match_string text
);


ALTER TABLE organic_chemical OWNER TO eichmann;

--
-- Name: organism; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE organism (
    id integer NOT NULL,
    organism text,
    umls_id text,
    umls_match_string text
);


ALTER TABLE organism OWNER TO eichmann;

--
-- Name: organization; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE organization (
    id integer NOT NULL,
    organization text,
    grid_id text,
    grid_match_string text,
    geonames_id integer,
    geonames_match_string text
);


ALTER TABLE organization OWNER TO eichmann;

--
-- Name: person; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE person (
    id integer NOT NULL,
    first_name text,
    last_name text,
    middle_name text,
    title text,
    appendix text
);


ALTER TABLE person OWNER TO eichmann;

--
-- Name: project; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE project (
    id integer NOT NULL,
    project text
);


ALTER TABLE project OWNER TO eichmann;

--
-- Name: provider; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE provider (
    id integer NOT NULL,
    person_id integer NOT NULL,
    resource_id integer NOT NULL
);


ALTER TABLE provider OWNER TO eichmann;

--
-- Name: publication_component; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE publication_component (
    id integer NOT NULL,
    publication_component text
);


ALTER TABLE publication_component OWNER TO eichmann;

--
-- Name: resource; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE resource (
    id integer NOT NULL,
    resource text,
    umls_id text,
    umls_match_string text,
    alt_umls_id text,
    alt_umls_match_string text
);


ALTER TABLE resource OWNER TO eichmann;

--
-- Name: skill; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE skill (
    id integer NOT NULL,
    person_id integer NOT NULL,
    technique_id integer NOT NULL
);


ALTER TABLE skill OWNER TO eichmann;

--
-- Name: support; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE support (
    id integer NOT NULL,
    support text
);


ALTER TABLE support OWNER TO eichmann;

--
-- Name: supporter; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE supporter (
    id integer NOT NULL,
    support_id integer NOT NULL,
    organization_id integer NOT NULL
);


ALTER TABLE supporter OWNER TO eichmann;

--
-- Name: technique; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE technique (
    id integer NOT NULL,
    technique text,
    umls_id text,
    umls_match_string text
);


ALTER TABLE technique OWNER TO eichmann;

--
-- Name: extraction_statistics; Type: MATERIALIZED VIEW; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE MATERIALIZED VIEW extraction_statistics AS
 SELECT 'person'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM person
UNION
 SELECT 'organization'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM organization
UNION
 SELECT 'award'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM award
UNION
 SELECT 'collaboration'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM collaboration
UNION
 SELECT 'disease'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM disease
UNION
 SELECT 'organic chemical'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM organic_chemical
UNION
 SELECT 'resource'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM resource
UNION
 SELECT 'support'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM support
UNION
 SELECT 'technique'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM technique
UNION
 SELECT 'location'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM location
UNION
 SELECT 'organism'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM organism
UNION
 SELECT 'project'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM project
UNION
 SELECT 'funding agency'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM funding_agency
UNION
 SELECT 'discipline'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM discipline
UNION
 SELECT 'publication component'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM publication_component
UNION
 SELECT 'event'::text AS name,
    'entity'::text AS mode,
    count(*) AS count
   FROM event
UNION
 SELECT 'funder'::text AS name,
    'relationship'::text AS mode,
    count(*) AS count
   FROM funder
UNION
 SELECT 'collaborant'::text AS name,
    'relationship'::text AS mode,
    count(*) AS count
   FROM collaborant
UNION
 SELECT 'collaborator'::text AS name,
    'relationship'::text AS mode,
    count(*) AS count
   FROM collaborator
UNION
 SELECT 'investigator'::text AS name,
    'relationship'::text AS mode,
    count(*) AS count
   FROM investigator
UNION
 SELECT 'affiliation'::text AS name,
    'relationship'::text AS mode,
    count(*) AS count
   FROM affiliation
UNION
 SELECT 'provider'::text AS name,
    'relationship'::text AS mode,
    count(*) AS count
   FROM provider
UNION
 SELECT 'supporter'::text AS name,
    'relationship'::text AS mode,
    count(*) AS count
   FROM supporter
UNION
 SELECT 'skill'::text AS name,
    'relationship'::text AS mode,
    count(*) AS count
   FROM skill
UNION
 SELECT 'awardee'::text AS name,
    'relationship'::text AS mode,
    count(*) AS count
   FROM awardee
  WITH NO DATA;


ALTER TABLE extraction_statistics OWNER TO eichmann;

--
-- Name: fragment; Type: VIEW; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE VIEW fragment AS
 SELECT ack_fragment.id AS id,
    ack_fragment.seqnum,
    ack_fragment.sentnum,
    ack_fragment.node,
    ack_fragment.fragment
   FROM ack_fragment;


ALTER TABLE fragment OWNER TO eichmann;

--
-- Name: fragments; Type: MATERIALIZED VIEW; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE MATERIALIZED VIEW fragments AS
 SELECT fragment.fragment,
    count(*) AS frequency
   FROM fragment
  GROUP BY fragment.fragment
  WITH NO DATA;


ALTER TABLE fragments OWNER TO eichmann;

--
-- Name: funding_agency_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE funding_agency_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE funding_agency_id_seq OWNER TO eichmann;

--
-- Name: funding_agency_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE funding_agency_id_seq OWNED BY funding_agency.id;


--
-- Name: ignore; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE ignore (
    id integer
);


ALTER TABLE ignore OWNER TO eichmann;

--
-- Name: location_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE location_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE location_id_seq OWNER TO eichmann;

--
-- Name: location_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE location_id_seq OWNED BY location.id;


--
-- Name: organic_chemical_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE organic_chemical_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE organic_chemical_id_seq OWNER TO eichmann;

--
-- Name: organic_chemical_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE organic_chemical_id_seq OWNED BY organic_chemical.id;


--
-- Name: organism_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE organism_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE organism_id_seq OWNER TO eichmann;

--
-- Name: organism_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE organism_id_seq OWNED BY organism.id;


--
-- Name: organization_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE organization_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE organization_id_seq OWNER TO eichmann;

--
-- Name: organization_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE organization_id_seq OWNED BY organization.id;


--
-- Name: person_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE person_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE person_id_seq OWNER TO eichmann;

--
-- Name: person_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE person_id_seq OWNED BY person.id;


--
-- Name: project_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE project_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE project_id_seq OWNER TO eichmann;

--
-- Name: project_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE project_id_seq OWNED BY project.id;


--
-- Name: publication_component_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE publication_component_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE publication_component_id_seq OWNER TO eichmann;

--
-- Name: publication_component_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE publication_component_id_seq OWNED BY publication_component.id;


--
-- Name: refragment; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE refragment (
    id integer
);


ALTER TABLE refragment OWNER TO eichmann;

--
-- Name: resource_category; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE resource_category (
    category text
);


ALTER TABLE resource_category OWNER TO eichmann;

--
-- Name: resource_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE resource_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE resource_id_seq OWNER TO eichmann;

--
-- Name: resource_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE resource_id_seq OWNED BY resource.id;


--
-- Name: support_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE support_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE support_id_seq OWNER TO eichmann;

--
-- Name: support_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE support_id_seq OWNED BY support.id;


--
-- Name: technique_id_seq; Type: SEQUENCE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE SEQUENCE technique_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE technique_id_seq OWNER TO eichmann;

--
-- Name: technique_id_seq; Type: SEQUENCE OWNED BY; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER SEQUENCE technique_id_seq OWNED BY technique.id;


--
-- Name: template; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE template (
    fragment text,
    tgrep text,
    mode text,
    relation text,
    slot0 text,
    slot1 text,
    slot2 text,
    slot3 text,
    slot4 text,
    slot5 text,
    slot6 text,
    slot7 text,
    slot8 text,
    slot9 text,
    instances integer
);


ALTER TABLE template OWNER TO eichmann;

--
-- Name: template_backup; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE template_backup (
    fragment text,
    tgrep text,
    mode text,
    relation text,
    slot0 text,
    slot1 text,
    slot2 text,
    slot3 text,
    slot4 text,
    slot5 text,
    slot6 text,
    slot7 text,
    slot8 text,
    slot9 text,
    instances integer
);


ALTER TABLE template_backup OWNER TO eichmann;

--
-- Name: template_complete; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE template_complete (
    fragment text
);


ALTER TABLE template_complete OWNER TO eichmann;

--
-- Name: template_defer; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE template_defer (
    fragment text
);


ALTER TABLE template_defer OWNER TO eichmann;

--
-- Name: template_suppress; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE template_suppress (
    fragment text
);


ALTER TABLE template_suppress OWNER TO eichmann;

--
-- Name: template_suppress_backup; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE template_suppress_backup (
    fragment text
);


ALTER TABLE template_suppress_backup OWNER TO eichmann;

--
-- Name: token; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE token (
    id integer,
    ind integer,
    token text
);


ALTER TABLE token OWNER TO eichmann;

--
-- Name: token_ack; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE token_ack (
    id integer,
    ind integer,
    token text
);


ALTER TABLE token_ack OWNER TO eichmann;

--
-- Name: token_ack_freq; Type: MATERIALIZED VIEW; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE MATERIALIZED VIEW token_ack_freq AS
 SELECT token_ack.ind,
    token_ack.token,
    count(*) AS count
   FROM token_ack
  GROUP BY token_ack.ind, token_ack.token
  WITH NO DATA;


ALTER TABLE token_ack_freq OWNER TO eichmann;

--
-- Name: token_baseline; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE token_baseline (
    id integer,
    ind integer,
    token text
);


ALTER TABLE token_baseline OWNER TO eichmann;

--
-- Name: token_baseline_freq; Type: MATERIALIZED VIEW; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE MATERIALIZED VIEW token_baseline_freq AS
 SELECT token_baseline.ind,
    token_baseline.token,
    count(*) AS count
   FROM token_baseline
  GROUP BY token_baseline.ind, token_baseline.token
  WITH NO DATA;


ALTER TABLE token_baseline_freq OWNER TO eichmann;

--
-- Name: token_mod; Type: TABLE; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE TABLE token_mod (
    id integer,
    ind integer,
    token text
);


ALTER TABLE token_mod OWNER TO eichmann;

--
-- Name: token_mod_freq; Type: MATERIALIZED VIEW; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE MATERIALIZED VIEW token_mod_freq AS
 SELECT token_mod.ind,
    token_mod.token,
    count(*) AS count
   FROM token_mod
  GROUP BY token_mod.ind, token_mod.token
  WITH NO DATA;


ALTER TABLE token_mod_freq OWNER TO eichmann;

--
-- Name: award id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY award ALTER COLUMN id SET DEFAULT nextval('award_id_seq'::regclass);


--
-- Name: collaboration id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY collaboration ALTER COLUMN id SET DEFAULT nextval('collaboration_id_seq'::regclass);


--
-- Name: discipline id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY discipline ALTER COLUMN id SET DEFAULT nextval('discipline_id_seq'::regclass);


--
-- Name: disease id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY disease ALTER COLUMN id SET DEFAULT nextval('disease_id_seq'::regclass);


--
-- Name: event id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY event ALTER COLUMN id SET DEFAULT nextval('event_id_seq'::regclass);


--
-- Name: funding_agency id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY funding_agency ALTER COLUMN id SET DEFAULT nextval('funding_agency_id_seq'::regclass);


--
-- Name: location id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY location ALTER COLUMN id SET DEFAULT nextval('location_id_seq'::regclass);


--
-- Name: organic_chemical id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organic_chemical ALTER COLUMN id SET DEFAULT nextval('organic_chemical_id_seq'::regclass);


--
-- Name: organism id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organism ALTER COLUMN id SET DEFAULT nextval('organism_id_seq'::regclass);


--
-- Name: organization id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organization ALTER COLUMN id SET DEFAULT nextval('organization_id_seq'::regclass);


--
-- Name: person id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY person ALTER COLUMN id SET DEFAULT nextval('person_id_seq'::regclass);


--
-- Name: project id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY project ALTER COLUMN id SET DEFAULT nextval('project_id_seq'::regclass);


--
-- Name: publication_component id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY publication_component ALTER COLUMN id SET DEFAULT nextval('publication_component_id_seq'::regclass);


--
-- Name: resource id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY resource ALTER COLUMN id SET DEFAULT nextval('resource_id_seq'::regclass);


--
-- Name: support id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY support ALTER COLUMN id SET DEFAULT nextval('support_id_seq'::regclass);


--
-- Name: technique id; Type: DEFAULT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY technique ALTER COLUMN id SET DEFAULT nextval('technique_id_seq'::regclass);


--
-- Name: award award_award_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY award
    ADD CONSTRAINT award_award_key UNIQUE (award);


--
-- Name: award award_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY award
    ADD CONSTRAINT award_pkey PRIMARY KEY (id);


--
-- Name: awardee awardee_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY awardee
    ADD CONSTRAINT awardee_pkey PRIMARY KEY (id, award_id, person_id);


--
-- Name: collaborant collaborant_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY collaborant
    ADD CONSTRAINT collaborant_pkey PRIMARY KEY (id, organization_id, collaboration_id);


--
-- Name: collaboration collaboration_collaboration_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY collaboration
    ADD CONSTRAINT collaboration_collaboration_key UNIQUE (collaboration);


--
-- Name: collaboration collaboration_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY collaboration
    ADD CONSTRAINT collaboration_pkey PRIMARY KEY (id);


--
-- Name: collaborator collaborator_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY collaborator
    ADD CONSTRAINT collaborator_pkey PRIMARY KEY (id, collaboration_id, person_id);


--
-- Name: discipline discipline_discipline_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY discipline
    ADD CONSTRAINT discipline_discipline_key UNIQUE (discipline);


--
-- Name: discipline discipline_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY discipline
    ADD CONSTRAINT discipline_pkey PRIMARY KEY (id);


--
-- Name: disease disease_disease_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY disease
    ADD CONSTRAINT disease_disease_key UNIQUE (disease);


--
-- Name: disease disease_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY disease
    ADD CONSTRAINT disease_pkey PRIMARY KEY (id);


--
-- Name: event event_event_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY event
    ADD CONSTRAINT event_event_key UNIQUE (event);


--
-- Name: event event_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY event
    ADD CONSTRAINT event_pkey PRIMARY KEY (id);


--
-- Name: funder funder_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY funder
    ADD CONSTRAINT funder_pkey PRIMARY KEY (id, organization_id, award_id);


--
-- Name: funding_agency funding_agency_funding_agency_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY funding_agency
    ADD CONSTRAINT funding_agency_funding_agency_key UNIQUE (funding_agency);


--
-- Name: funding_agency funding_agency_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY funding_agency
    ADD CONSTRAINT funding_agency_pkey PRIMARY KEY (id);


--
-- Name: investigator investigator_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY investigator
    ADD CONSTRAINT investigator_pkey PRIMARY KEY (id, person_id, organization_id);


--
-- Name: location location_location_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY location
    ADD CONSTRAINT location_location_key UNIQUE (location);


--
-- Name: location location_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY location
    ADD CONSTRAINT location_pkey PRIMARY KEY (id);


--
-- Name: award_mention mention_award_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY award_mention
    ADD CONSTRAINT mention_award_pkey PRIMARY KEY (award_id, id);


--
-- Name: collaboration_mention mention_collaboration_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY collaboration_mention
    ADD CONSTRAINT mention_collaboration_pkey PRIMARY KEY (collaboration_id, id);


--
-- Name: discipline_mention mention_discipline_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY discipline_mention
    ADD CONSTRAINT mention_discipline_pkey PRIMARY KEY (discipline_id, id);


--
-- Name: disease_mention mention_disease_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY disease_mention
    ADD CONSTRAINT mention_disease_pkey PRIMARY KEY (disease_id, id);


--
-- Name: event_mention mention_event_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY event_mention
    ADD CONSTRAINT mention_event_pkey PRIMARY KEY (event_id, id);


--
-- Name: funding_agency_mention mention_funding_agency_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY funding_agency_mention
    ADD CONSTRAINT mention_funding_agency_pkey PRIMARY KEY (funding_agency_id, id);


--
-- Name: location_mention mention_location_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY location_mention
    ADD CONSTRAINT mention_location_pkey PRIMARY KEY (location_id, id);


--
-- Name: organic_chemical_mention mention_organic_chemical_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organic_chemical_mention
    ADD CONSTRAINT mention_organic_chemical_pkey PRIMARY KEY (organic_chemical_id, id);


--
-- Name: organism_mention mention_organism_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organism_mention
    ADD CONSTRAINT mention_organism_pkey PRIMARY KEY (organism_id, id);


--
-- Name: organization_mention mention_organization_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organization_mention
    ADD CONSTRAINT mention_organization_pkey PRIMARY KEY (organization_id);


--
-- Name: person_mention mention_person_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY person_mention
    ADD CONSTRAINT mention_person_pkey PRIMARY KEY (person_id, id);


--
-- Name: project_mention mention_project_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY project_mention
    ADD CONSTRAINT mention_project_pkey PRIMARY KEY (project_id, id);


--
-- Name: publication_component_mention mention_publication_component_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY publication_component_mention
    ADD CONSTRAINT mention_publication_component_pkey PRIMARY KEY (publication_component_id, id);


--
-- Name: resource_mention mention_resource_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY resource_mention
    ADD CONSTRAINT mention_resource_pkey PRIMARY KEY (resource_id, id);


--
-- Name: support_mention mention_support_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY support_mention
    ADD CONSTRAINT mention_support_pkey PRIMARY KEY (support_id, id);


--
-- Name: technique_mention mention_technique_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY technique_mention
    ADD CONSTRAINT mention_technique_pkey PRIMARY KEY (technique_id, id);


--
-- Name: organic_chemical organic_chemical_organic_chemical_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organic_chemical
    ADD CONSTRAINT organic_chemical_organic_chemical_key UNIQUE (organic_chemical);


--
-- Name: organic_chemical organic_chemical_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organic_chemical
    ADD CONSTRAINT organic_chemical_pkey PRIMARY KEY (id);


--
-- Name: organism organism_organism_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organism
    ADD CONSTRAINT organism_organism_key UNIQUE (organism);


--
-- Name: organism organism_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organism
    ADD CONSTRAINT organism_pkey PRIMARY KEY (id);


--
-- Name: organization organization_organization_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organization
    ADD CONSTRAINT organization_organization_key UNIQUE (organization);


--
-- Name: organization organization_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organization
    ADD CONSTRAINT organization_pkey PRIMARY KEY (id);


--
-- Name: person person_first_name_last_name_middle_name_title_appendix_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY person
    ADD CONSTRAINT person_first_name_last_name_middle_name_title_appendix_key UNIQUE (first_name, last_name, middle_name, title, appendix);


--
-- Name: person person_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY person
    ADD CONSTRAINT person_pkey PRIMARY KEY (id);


--
-- Name: project project_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY project
    ADD CONSTRAINT project_pkey PRIMARY KEY (id);


--
-- Name: project project_project_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY project
    ADD CONSTRAINT project_project_key UNIQUE (project);


--
-- Name: provider provider_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY provider
    ADD CONSTRAINT provider_pkey PRIMARY KEY (id, person_id, resource_id);


--
-- Name: publication_component publication_component_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY publication_component
    ADD CONSTRAINT publication_component_pkey PRIMARY KEY (id);


--
-- Name: publication_component publication_component_publication_component_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY publication_component
    ADD CONSTRAINT publication_component_publication_component_key UNIQUE (publication_component);


--
-- Name: resource resource_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY resource
    ADD CONSTRAINT resource_pkey PRIMARY KEY (id);


--
-- Name: resource resource_resource_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY resource
    ADD CONSTRAINT resource_resource_key UNIQUE (resource);


--
-- Name: skill skill_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY skill
    ADD CONSTRAINT skill_pkey PRIMARY KEY (id, person_id, technique_id);


--
-- Name: support support_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY support
    ADD CONSTRAINT support_pkey PRIMARY KEY (id);


--
-- Name: support support_support_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY support
    ADD CONSTRAINT support_support_key UNIQUE (support);


--
-- Name: supporter supporter_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY supporter
    ADD CONSTRAINT supporter_pkey PRIMARY KEY (id, support_id, organization_id);


--
-- Name: technique technique_pkey; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY technique
    ADD CONSTRAINT technique_pkey PRIMARY KEY (id);


--
-- Name: technique technique_technique_key; Type: CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY technique
    ADD CONSTRAINT technique_technique_key UNIQUE (technique);


--
-- Name: abdec; Type: INDEX; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE INDEX abdec ON ack_by_distinct_entity USING btree (count);


--
-- Name: abtes; Type: INDEX; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE INDEX abtes ON ack_by_total_entity USING btree (sum);


--
-- Name: aff; Type: INDEX; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE INDEX aff ON ack_fragment USING btree (fragment);


--
-- Name: afp; Type: INDEX; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE INDEX afp ON ack_fragment USING btree (id);


--
-- Name: afs; Type: INDEX; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE INDEX afs ON fragments USING btree (frequency);


--
-- Name: app; Type: INDEX; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE INDEX app ON ack_parse USING btree (id);


--
-- Name: asp; Type: INDEX; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE INDEX asp ON ack_sentence USING btree (id);


--
-- Name: ffs; Type: INDEX; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

CREATE INDEX ffs ON fragments USING btree (fragment);


--
-- Name: awardee fk_awardee_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY awardee
    ADD CONSTRAINT fk_awardee_1 FOREIGN KEY (award_id) REFERENCES award(id);


--
-- Name: awardee fk_awardee_2; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY awardee
    ADD CONSTRAINT fk_awardee_2 FOREIGN KEY (person_id) REFERENCES person(id);


--
-- Name: collaborant fk_collaborant_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY collaborant
    ADD CONSTRAINT fk_collaborant_1 FOREIGN KEY (organization_id) REFERENCES organization(id);


--
-- Name: collaborant fk_collaborant_2; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY collaborant
    ADD CONSTRAINT fk_collaborant_2 FOREIGN KEY (collaboration_id) REFERENCES collaboration(id);


--
-- Name: collaborator fk_collaborator_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY collaborator
    ADD CONSTRAINT fk_collaborator_1 FOREIGN KEY (collaboration_id) REFERENCES collaboration(id);


--
-- Name: collaborator fk_collaborator_2; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY collaborator
    ADD CONSTRAINT fk_collaborator_2 FOREIGN KEY (person_id) REFERENCES person(id);


--
-- Name: funder fk_funder_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY funder
    ADD CONSTRAINT fk_funder_1 FOREIGN KEY (organization_id) REFERENCES organization(id);


--
-- Name: funder fk_funder_2; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY funder
    ADD CONSTRAINT fk_funder_2 FOREIGN KEY (award_id) REFERENCES award(id);


--
-- Name: investigator fk_investigator_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY investigator
    ADD CONSTRAINT fk_investigator_1 FOREIGN KEY (organization_id) REFERENCES organization(id);


--
-- Name: investigator fk_investigator_2; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY investigator
    ADD CONSTRAINT fk_investigator_2 FOREIGN KEY (person_id) REFERENCES person(id);


--
-- Name: provider fk_provider_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY provider
    ADD CONSTRAINT fk_provider_1 FOREIGN KEY (person_id) REFERENCES person(id);


--
-- Name: provider fk_provider_2; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY provider
    ADD CONSTRAINT fk_provider_2 FOREIGN KEY (resource_id) REFERENCES resource(id);


--
-- Name: supporter fk_supporter_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY supporter
    ADD CONSTRAINT fk_supporter_1 FOREIGN KEY (support_id) REFERENCES support(id);


--
-- Name: supporter fk_supporter_2; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY supporter
    ADD CONSTRAINT fk_supporter_2 FOREIGN KEY (organization_id) REFERENCES organization(id);


--
-- Name: organism_mention fk_table_24_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organism_mention
    ADD CONSTRAINT fk_table_24_1 FOREIGN KEY (organism_id) REFERENCES organism(id);


--
-- Name: technique_mention fk_table_25_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY technique_mention
    ADD CONSTRAINT fk_table_25_1 FOREIGN KEY (technique_id) REFERENCES technique(id);


--
-- Name: support_mention fk_table_26_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY support_mention
    ADD CONSTRAINT fk_table_26_1 FOREIGN KEY (support_id) REFERENCES support(id);


--
-- Name: resource_mention fk_table_27_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY resource_mention
    ADD CONSTRAINT fk_table_27_1 FOREIGN KEY (resource_id) REFERENCES resource(id);


--
-- Name: location_mention fk_table_28_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY location_mention
    ADD CONSTRAINT fk_table_28_1 FOREIGN KEY (location_id) REFERENCES location(id);


--
-- Name: disease_mention fk_table_29_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY disease_mention
    ADD CONSTRAINT fk_table_29_1 FOREIGN KEY (disease_id) REFERENCES disease(id);


--
-- Name: organic_chemical_mention fk_table_30_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organic_chemical_mention
    ADD CONSTRAINT fk_table_30_1 FOREIGN KEY (organic_chemical_id) REFERENCES organic_chemical(id);


--
-- Name: collaboration_mention fk_table_31_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY collaboration_mention
    ADD CONSTRAINT fk_table_31_1 FOREIGN KEY (collaboration_id) REFERENCES collaboration(id);


--
-- Name: award_mention fk_table_32_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY award_mention
    ADD CONSTRAINT fk_table_32_1 FOREIGN KEY (award_id) REFERENCES award(id);


--
-- Name: person_mention fk_table_33_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY person_mention
    ADD CONSTRAINT fk_table_33_1 FOREIGN KEY (person_id) REFERENCES person(id);


--
-- Name: organization_mention fk_table_34_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY organization_mention
    ADD CONSTRAINT fk_table_34_1 FOREIGN KEY (organization_id) REFERENCES organization(id);


--
-- Name: event_mention fk_table_35_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY event_mention
    ADD CONSTRAINT fk_table_35_1 FOREIGN KEY (event_id) REFERENCES event(id);


--
-- Name: publication_component_mention fk_table_36_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY publication_component_mention
    ADD CONSTRAINT fk_table_36_1 FOREIGN KEY (publication_component_id) REFERENCES publication_component(id);


--
-- Name: discipline_mention fk_table_37_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY discipline_mention
    ADD CONSTRAINT fk_table_37_1 FOREIGN KEY (discipline_id) REFERENCES discipline(id);


--
-- Name: project_mention fk_table_39_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY project_mention
    ADD CONSTRAINT fk_table_39_1 FOREIGN KEY (project_id) REFERENCES project(id);


--
-- Name: funding_agency_mention fk_table_40_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY funding_agency_mention
    ADD CONSTRAINT fk_table_40_1 FOREIGN KEY (funding_agency_id) REFERENCES funding_agency(id);


--
-- Name: skill fk_table_41_1; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY skill
    ADD CONSTRAINT fk_table_41_1 FOREIGN KEY (person_id) REFERENCES person(id);


--
-- Name: skill fk_table_41_2; Type: FK CONSTRAINT; Schema: pubmed_central_ack_stanford; Owner: eichmann
--

ALTER TABLE ONLY skill
    ADD CONSTRAINT fk_table_41_2 FOREIGN KEY (technique_id) REFERENCES technique(id);


--
-- PostgreSQL database dump complete
--

