-- HSQLDB setup script

drop table propertyvalues if exists;
drop table nodes if exists;
drop table workspaces if exists;

create memory table workspaces (
	id 		integer 		identity primary key,
	name	varchar(100) 	not null
	
);

-- create unique index idx_workspaces_name on workspaces(name);

create table nodes (
	id		bigint 			identity primary key,
	workspace integer		not null,
	path	varchar(2048) 	not null,
	parent	bigint,
	uuid	varchar(36),

	foreign key (workspace)	references workspaces (id),
	foreign key (parent)	references nodes(id),
	constraint uniq_path 	unique(workspace,path)
);

create index idx_nodes_path on nodes(path);
create index idx_nodes_id   on nodes(id,path);

create table propertyvalues (
	id		bigint			identity primary key,
	parent	bigint 			not null,
	name	varchar(128)	not null,
	type	tinyint			not null,
	multi   boolean         not null,
	len     bigint,
	propval	binary,
	
	foreign key (parent)	references nodes (id),
	constraint uniq_prop    unique(parent,name)
);

create index idx_propertyvalues_name on propertyvalues(name);

insert into workspaces (
	name
) values (
	'default'
);


insert into workspaces (
	name
) values (
	'testworkspace'
);
