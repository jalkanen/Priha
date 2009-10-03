use test;

drop table if exists propertyvalues;

drop table if exists nodes;

drop table if exists workspaces;

create table workspaces (
    id 	    integer         auto_increment primary key,
    name    varchar(100)    not null
);

create unique index idx_workspaces_name on workspaces(name);

create table nodes (
	id		integer 		auto_increment primary key,
	workspace integer		not null,
	path	varchar(768) 	not null,
	parent	integer,
	uuid	varchar(36),
    childOrder integer,
    
	foreign key (workspace)	references workspaces (id),
--	foreign key (parent)	references nodes(id),
	constraint uniq_path 	unique(workspace,path)
);

create index idx_nodes_path on nodes(workspace,path);
create index idx_nodes_id   on nodes(id,path);
create index idx_uuid       on nodes(uuid);
create index idx_parent     on nodes(workspace,parent);

create table propertyvalues (
	id		integer			auto_increment primary key,
	parent	integer 		not null,
	name	varchar(128)	not null,
	type	tinyint			not null,
	multi   boolean         not null,
	len     bigint,
	propval	binary,
	
--	foreign key (parent)	references nodes (id),
	constraint uniq_prop    unique(parent,name)
);

create index idx_propertyvalues_name on propertyvalues(name);
