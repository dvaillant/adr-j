# 0006. ADRs can be created using different templates

Date: 04-Nov-2017

## Status

Accepted



## Context

Not all users will want to use the Nygard ADR form for ADRs, for instance, corporate developers may have to use their employees standards for ADRS

## Decision

When initializing a project for ADRs the developer can specify the template to use for the ADR, e.g.

    adr init [template file location]

If not specified the Nygard template is used as default.

## Consequences

* The tool offers more flexibility
* The templates have to be defined and a syntax developed.
* Not all templates may specify the "standard fields" such as date, supersedes etc.. As such the tool should allow for this.
* Some templates may required more "standard fields" than supported by the tool. As such the tool can either support a subset of the template or not support the template at all.  
