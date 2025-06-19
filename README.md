test

<h1 align="center">
  <img src="https://github.com/KaotoIO/kaoto.io/blob/main/assets/media/logo-kaoto.png" alt="Kaoto">
</h1>

<p align=center>
  <a href="https://github.com/KaotoIO/catalogs/blob/main/LICENSE"><img src="https://img.shields.io/github/license/KaotoIO/catalogs?color=blue&style=for-the-badge" alt="License"/></a>
  <a href="https://www.youtube.com/@KaotoIO"><img src="https://img.shields.io/badge/Youtube-Follow-brightgreen?color=red&style=for-the-badge" alt="Youtube"" alt="Follow on Youtube"></a>
  <a href="https://camel.zulipchat.com/#narrow/stream/441302-kaoto"><img src="https://img.shields.io/badge/zulip-join_chat-brightgreen?color=yellow&style=for-the-badge" alt="Zulip"/></a>
  <a href="https://kaoto.io"><img src="https://img.shields.io/badge/Kaoto.io-Visit-white?color=indigo&style=for-the-badge" alt="Zulip"/></a>
</p><br/>

<h2 align="center">Kaoto - The Integration Designer for <a href="https://camel.apache.org">Apache Camel</a></h2>

<p align="center">
  <a href="https://kaoto.io/docs/installation">Documentation</a> | 
  <a href="https://kaoto.io/workshop/">Workshops</a> | 
  <a href="https://kaoto.io/contribute/">Contribute</a> | 
  <a href="https://camel.zulipchat.com/#narrow/stream/441302-kaoto">Chat</a>
</p>

# Kaoto
Kaoto is a visual editor for Apache Camel integrations. It offers support in creating and editing Camel Routes, Kamelets and Pipes. Kaoto also has a built-in catalog with available Camel components, Enterprise Integration Patterns and Kamelets provided by the Apache Camel community.

Have a quick look at our online demo instance:
https://kaotoio.github.io/kaoto/

# Catalogs
This repository holds available catalogs for use in Kaoto.

## Camel Catalog and Supporting Schemas
To build the Camel Catalog and the supporting schemas, run:
```sh
yarn workspace @kaoto/camel-catalog run build
```
or 
```sh
yarn install
```

_Optional: You can update the Camel version in the `pom.xml` file and then run the build command again._
