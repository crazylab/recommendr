--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: new_trx; Type: TABLE; Schema: public; Owner: dileepba; Tablespace: 
--

CREATE TABLE new_trx (
    user_id character varying(200),
    product_id text
);


ALTER TABLE new_trx OWNER TO dileepba;

--
-- Name: product_score; Type: TABLE; Schema: public; Owner: dileepba; Tablespace: 
--

CREATE TABLE product_score (
    product_id1 character varying(200),
    product_id2 character varying(200),
    score double precision
);


ALTER TABLE product_score OWNER TO dileepba;

--
-- Name: products; Type: TABLE; Schema: public; Owner: dileepba; Tablespace: 
--

CREATE TABLE products (
    product_id character varying(200),
    description character varying(200),
    sub_desc character varying(200),
    manufacturer character varying(200),
    dept character varying(200),
    brand character varying(200),
    size character varying(200)
);


ALTER TABLE products OWNER TO dileepba;

--
-- Name: products; Type: ACL; Schema: public; Owner: dileepba
--

REVOKE ALL ON TABLE products FROM PUBLIC;
REVOKE ALL ON TABLE products FROM dileepba;
GRANT ALL ON TABLE products TO dileepba;
GRANT ALL ON TABLE products TO test;


--
-- PostgreSQL database dump complete
--

