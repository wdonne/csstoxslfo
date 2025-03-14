<?xml version='1.0' encoding='UTF-8'?>
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:css="http://www.w3.org/1998/CSS"
  xmlns:sp="urn:be-re-css:specificity" xmlns:rx="http://www.renderx.com/XSL/Extensions"
  xmlns:axf="http://www.antennahouse.com/names/XSL/Extensions" xmlns:xmp="adobe:ns:meta/"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:dc="http://purl.org/dc/elements/1.1/" version="2.0" exclude-result-prefixes="css sp">

  <xsl:include href="fo_setup.xsl"/>
  <xsl:include href="util.xsl"/>

  <xsl:output method="xml" indent="no" version="1.0" encoding="UTF-8" omit-xml-declaration="no"/>


  <!-- User-defined page set-up. -->

  <xsl:template match="/css:root[css:pages]" mode="setup">
    <fo:root font-selection-strategy="character-by-character"
      line-height-shift-adjustment="disregard-shifts" font-family="{$font-family}"
      font-size="{$actual-font-size}" language="{$language}">
      <!--
        The document element's properties are put here for inheritance
        purposes. The display property has no effect because pages are
        generated for the top-level elements.
      -->
      <xsl:apply-templates select="@*"/>
      <xsl:if test="not(@xml:lang) and not(@lang)">
        <xsl:attribute name="country">
          <xsl:value-of select="$country"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="css:meta-data" mode="rx"/>
      <xsl:apply-templates select="css:meta-data" mode="axf"/>
      <fo:layout-master-set>
        <xsl:apply-templates select="css:pages/css:page"/>
        <!--
          If a named page exists then it is always split in six specific
          versions.
        -->
        <xsl:apply-templates select="css:pages/css:page[starts-with(@css:name, 'first-left-')]"
          mode="master"/>
      </fo:layout-master-set>
      <fo:declarations>
        <xsl:apply-templates select="css:meta-data" mode="xmp"/>
      </fo:declarations>
      <xsl:apply-templates select=".//fo:bookmark-tree" mode="move"/>
      <xsl:apply-templates select="css:page-sequence"/>
    </fo:root>
  </xsl:template>


  <xsl:template match="css:page[starts-with(@css:name, 'first-left-')]" mode="master">
    <xsl:variable name="name" select="substring-after(@css:name, 'first-left-')"/>
    <fo:page-sequence-master master-name="{$name}">
      <fo:repeatable-page-master-alternatives>
        <!--
          The following references are always generated. If the CSS style
          sheet hasn't specified them they will be equals to the named
          page definition.
        -->
        <fo:conditional-page-master-reference odd-or-even="odd" blank-or-not-blank="not-blank"
          page-position="first" master-reference="{concat('first-right-', $name)}"/>
        <fo:conditional-page-master-reference odd-or-even="even" blank-or-not-blank="not-blank"
          page-position="first" master-reference="{concat('first-left-', $name)}"/>
        <fo:conditional-page-master-reference odd-or-even="odd" blank-or-not-blank="not-blank"
          page-position="last" master-reference="{concat('last-right-', $name)}"/>
        <fo:conditional-page-master-reference odd-or-even="even" blank-or-not-blank="not-blank"
          page-position="last" master-reference="{concat('last-left-', $name)}"/>
        <fo:conditional-page-master-reference odd-or-even="odd" blank-or-not-blank="not-blank"
          page-position="any" master-reference="{concat('right-', $name)}"/>
        <fo:conditional-page-master-reference odd-or-even="even" blank-or-not-blank="not-blank"
          page-position="any" master-reference="{concat('left-', $name)}"/>
        <fo:conditional-page-master-reference odd-or-even="odd" blank-or-not-blank="blank"
          page-position="any" master-reference="{concat('blank-right-', $name)}"/>
        <fo:conditional-page-master-reference odd-or-even="even" blank-or-not-blank="blank"
          page-position="any" master-reference="{concat('blank-left-', $name)}"/>
      </fo:repeatable-page-master-alternatives>
    </fo:page-sequence-master>
  </xsl:template>


  <xsl:template match="css:page">
    <fo:simple-page-master master-name="{@css:name}" writing-mode="{$writing-mode}"
      page-height="{$paper-height}" page-width="{$paper-width}" margin-top="{$paper-margin-top}"
      margin-bottom="{$paper-margin-bottom}" margin-left="{$paper-margin-left}"
      margin-right="{$paper-margin-right}">
      <xsl:apply-templates
        select="@*[name() != 'css:column-count' and name() != 'css:force-page-count' and name() != 'css:initial-page-number' and name() != 'css:column-gap']"/>
      <xsl:apply-templates select="*"/>
    </fo:simple-page-master>
  </xsl:template>


  <xsl:template match="css:page/@css:force-page-count" priority="1">
    <xsl:attribute name="force-page-count">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="css:page/@css:initial-page-number" priority="1">
    <xsl:attribute name="initial-page-number">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <!-- Don't generate empty page sequences. -->

  <xsl:template
    match="css:page-sequence[(not(css:regions/following-sibling::*//*[(@css:display = 'block' or @css:display = 'table') and @css:page]/*) and not(css:regions/following-sibling::*//*[@css:display = 'block' and @css:page]/text()) and not(css:regions/following-sibling::*//*[(@css:display = 'block' or @css:display = 'table') and @css:page]/following-sibling::*)  and @css:page != 'unnamed') or (not(*[@css:display = 'block' or @css:display = 'table']/*) and not(*[@css:display = 'block']/text()))]"/>


  <xsl:template match="css:page-sequence">
    <xsl:variable name="page-name" select="@css:page"/>
    <fo:page-sequence format="1" master-reference="unnamed">
      <xsl:apply-templates select="@css:page"/>
      <xsl:apply-templates
        select="/css:root/css:pages/css:page[@css:name = concat('first-left-', $page-name)]/@css:initial-page-number"/>
      <xsl:apply-templates
        select="/css:root/css:pages/css:page[@css:name = concat('first-left-', $page-name)]/@css:force-page-count"/>
      <xsl:apply-templates select="css:regions//css:page-number" mode="page-number-setup"/>
      <xsl:apply-templates select="css:regions/fo:static-content"/>
      <fo:static-content flow-name="xsl-footnote-separator">
        <xsl:call-template name="footnote-separator"/>
      </fo:static-content>
      <fo:flow flow-name="xsl-region-body">
        <xsl:apply-templates select="css:regions/following-sibling::*"/>
      </fo:flow>
    </fo:page-sequence>
  </xsl:template>


  <xsl:template match="css:page-sequence/@css:page" priority="1">
    <xsl:attribute name="master-reference">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <!-- Default orientation, i.e. defined from outside. -->
  <xsl:template match="css:page/@css:size[. = 'auto' or . = 'portrait']" priority="1"/>


  <xsl:template match="css:page/@css:size[. = 'landscape']" priority="1">
    <xsl:attribute name="page-height">
      <xsl:value-of select="$paper-width"/>
    </xsl:attribute>
    <xsl:attribute name="page-width">
      <xsl:value-of select="$paper-height"/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template
    match="css:page/@css:size[. != 'auto' and . != 'landscape' and . != 'portrait' and . != 'inherit']"
    priority="1">
    <xsl:variable name="tokens">
      <xsl:call-template name="count-tokens">
        <xsl:with-param name="s" select="."/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="$tokens = 2">
        <xsl:attribute name="page-width">
          <xsl:call-template name="get-token">
            <xsl:with-param name="position" select="1"/>
            <xsl:with-param name="s" select="."/>
          </xsl:call-template>
        </xsl:attribute>
        <xsl:attribute name="page-height">
          <xsl:call-template name="get-token">
            <xsl:with-param name="position" select="2"/>
            <xsl:with-param name="s" select="."/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute name="page-width">
          <xsl:value-of select="."/>
        </xsl:attribute>
        <xsl:attribute name="page-height">
          <xsl:value-of select="."/>
        </xsl:attribute>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <!-- Meta-data. -->

  <xsl:template match="css:meta-data" mode="rx">
    <rx:meta-info>
      <xsl:apply-templates select="css:field" mode="rx"/>
    </rx:meta-info>
  </xsl:template>


  <xsl:template match="css:field" mode="rx">
    <rx:meta-field>
      <xsl:attribute name="name">
        <xsl:value-of select="@css:name"/>
      </xsl:attribute>
      <xsl:attribute name="value">
        <xsl:value-of select="@css:value"/>
      </xsl:attribute>
    </rx:meta-field>
  </xsl:template>


  <xsl:template match="css:meta-data" mode="axf">
    <xsl:apply-templates select="css:field" mode="axf"/>
  </xsl:template>


  <xsl:template match="css:field" mode="axf">
    <axf:document-info>
      <xsl:attribute name="name">
        <xsl:value-of select="@css:name"/>
      </xsl:attribute>
      <xsl:attribute name="value">
        <xsl:value-of select="@css:value"/>
      </xsl:attribute>
    </axf:document-info>
  </xsl:template>


  <xsl:template match="css:meta-data" mode="xmp">
    <xmp:xmpmeta>
      <rdf:RDF>
        <rdf:Description rdf:about="">
          <xsl:apply-templates select="css:field" mode="xmp"/>
        </rdf:Description>
      </rdf:RDF>
    </xmp:xmpmeta>
  </xsl:template>


  <xsl:template match="css:field" mode="xmp">
    <xsl:element name="dc:{@css:name}">
      <xsl:value-of select="@css:value"/>
    </xsl:element>
  </xsl:template>


  <!-- General templates. -->

  <xsl:template match="@*" priority="-1"/>
  <xsl:template match="comment()"/>


  <xsl:template match="fo:*">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>


  <xsl:template match="fo:*/@*[not(starts-with(name(), 'css:'))]">
    <xsl:copy/>
  </xsl:template>


  <xsl:template match="fo:instream-foreign-object">
    <xsl:copy-of select="."/>
  </xsl:template>


  <xsl:template match="fo:bookmark-tree"/>


  <xsl:template match="fo:bookmark-tree" mode="move">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>


  <xsl:template match="@id | @ID | @xml:id" priority="1">
    <xsl:attribute name="id">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="@xml:lang[. != ''] | @lang[. != '']">
    <xsl:choose>
      <xsl:when test="contains(., '-')">
        <xsl:attribute name="language">
          <xsl:value-of select="substring-before(., '-')"/>
        </xsl:attribute>
        <xsl:attribute name="country">
          <xsl:value-of select="substring-after(., '-')"/>
        </xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute name="language">
          <xsl:value-of select="."/>
        </xsl:attribute>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template match="@css:*">
    <xsl:attribute name="{local-name()}">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="@css:content" priority="0.5"/>
  <xsl:template match="@css:counter-increment" priority="0.5"/>
  <xsl:template match="@css:counter-reset" priority="0.5"/>
  <xsl:template match="@css:cursor" priority="0.5"/>
  <xsl:template match="@css:display" priority="0.5"/>
  <xsl:template match="@css:force-page-count" priority="0.5"/>
  <xsl:template match="@css:format" priority="0.5"/>
  <xsl:template match="@css:initial-page-number" priority="0.5"/>
  <xsl:template match="@css:letter-value" priority="0.5"/>
  <xsl:template match="@css:list-label-width" priority="0.5"/>
  <xsl:template match="@css:list-style" priority="0.5"/>
  <xsl:template match="@css:list-style-image" priority="0.5"/>
  <xsl:template match="@css:list-style-position" priority="0.5"/>
  <xsl:template match="@css:list-style-type" priority="0.5"/>
  <xsl:template match="@css:marker-offset" priority="0.5"/>
  <xsl:template match="@css:marks" priority="0.5"/>
  <xsl:template match="@css:meta-data" priority="0.5"/>
  <xsl:template match="@css:name" priority="0.5"/>
  <xsl:template match="@css:outline" priority="0.5"/>
  <xsl:template match="@css:outline-color" priority="0.5"/>
  <xsl:template match="@css:outline-style" priority="0.5"/>
  <xsl:template match="@css:outline-width" priority="0.5"/>
  <xsl:template match="@css:page" priority="0.5"/>
  <xsl:template match="@css:precedence" priority="0.5"/>
  <xsl:template match="@css:quotes" priority="0.5"/>
  <xsl:template match="@css:region" priority="0.5"/>
  <xsl:template match="@css:span" priority="0.5"/>


  <xsl:template match="*[@css:display = 'block']">
    <fo:block margin-left="0pt" margin-right="0pt">
      <xsl:apply-templates select="@* | node()"/>
    </fo:block>
  </xsl:template>


  <xsl:template match="@css:orientation" priority="1">
    <xsl:attribute name="reference-orientation">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="@css:position[. = 'relative' or . = 'static']" priority="1">
    <xsl:attribute name="relative-position">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="@css:position[. = 'absolute' or . = 'fixed']" priority="1">
    <xsl:attribute name="absolute-position">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="*[@css:display = 'inline']">
    <fo:inline>
      <xsl:apply-templates select="@* | node()"/>
    </fo:inline>
  </xsl:template>


  <xsl:template match="*[@css:display = 'inline-block']">
    <fo:inline-container>
      <fo:block>
        <xsl:apply-templates select="@* | node()"/>
      </fo:block>
    </fo:inline-container>
  </xsl:template>


  <xsl:template match="*[@css:display = 'none']"/>


  <xsl:template match="*[@css:display = 'graphic' and @css:src != '']">
    <fo:external-graphic>
      <xsl:apply-templates
        select="@*[name() != 'css:color' and not(starts-with(name(), 'css:font')) and name() != 'css:line-height']"/>
    </fo:external-graphic>
  </xsl:template>


  <xsl:template match="@css:src" priority="1">
    <xsl:attribute name="src">
      <xsl:value-of select="concat('url(', ., ')')"/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="*[@css:display = 'list-item']">
    <xsl:variable name="list-style-type">
      <xsl:apply-templates select="." mode="list-style-type"/>
    </xsl:variable>
    <xsl:variable name="list-style-position">
      <xsl:apply-templates select="." mode="list-style-position"/>
    </xsl:variable>
    <fo:list-item>
      <xsl:choose>
        <xsl:when test="$list-style-type != 'none' and $list-style-position = 'outside'">
          <fo:list-item-label end-indent="label-end()">
            <fo:block text-align="end">
              <xsl:call-template name="list-label">
                <xsl:with-param name="list-type" select="$list-style-type"/>
              </xsl:call-template>
            </fo:block>
          </fo:list-item-label>
          <fo:list-item-body start-indent="body-start()">
            <fo:block>
              <xsl:apply-templates select="@* | node()"/>
            </fo:block>
          </fo:list-item-body>
        </xsl:when>
        <xsl:otherwise>
          <fo:list-item-label end-indent="0">
            <fo:block/>
          </fo:list-item-label>
          <fo:list-item-body start-indent="0">
            <fo:block>
              <xsl:apply-templates select="@*"/>
              <xsl:if test="$list-style-type != 'none'">
                <fo:inline>
                  <xsl:attribute name="width">
                    <xsl:apply-templates select="." mode="list-label-width">
                      <xsl:with-param name="list-type" select="$list-style-type"/>
                    </xsl:apply-templates>
                  </xsl:attribute>
                  <xsl:call-template name="list-label">
                    <xsl:with-param name="list-type" select="$list-style-type"/>
                  </xsl:call-template>
                </fo:inline>
              </xsl:if>
              <xsl:apply-templates select="node()"/>
            </fo:block>
          </fo:list-item-body>
        </xsl:otherwise>
      </xsl:choose>
    </fo:list-item>
  </xsl:template>


  <xsl:template match="*[*[@css:display = 'list-item']]" priority="1">
    <xsl:variable name="list-style-type">
      <xsl:apply-templates select="*[@css:display = 'list-item'][1]" mode="list-style-type"/>
    </xsl:variable>
    <xsl:variable name="list-style-position">
      <xsl:apply-templates select="*[@css:display = 'list-item'][1]" mode="list-style-position"/>
    </xsl:variable>
    <fo:list-block>
      <xsl:choose>
        <xsl:when test="$list-style-type != 'none' and $list-style-position = 'outside'">
          <xsl:variable name="label-width">
            <xsl:apply-templates select="*[@css:display = 'list-item'][1]" mode="list-label-width">
              <xsl:with-param name="list-type" select="$list-style-type"/>
            </xsl:apply-templates>
          </xsl:variable>
          <xsl:choose>
            <xsl:when test="$label-width = ''">
              <xsl:attribute name="provisional-label-separation">
                <xsl:text>0</xsl:text>
              </xsl:attribute>
              <xsl:attribute name="provisional-distance-between-starts">
                <xsl:text>0</xsl:text>
              </xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
              <xsl:attribute name="provisional-label-separation">
                <xsl:text>6pt</xsl:text>
              </xsl:attribute>
              <xsl:attribute name="provisional-distance-between-starts">
                <xsl:value-of select="$label-width"/>
              </xsl:attribute>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="provisional-label-separation">
            <xsl:text>0</xsl:text>
          </xsl:attribute>
          <xsl:attribute name="provisional-distance-between-starts">
            <xsl:text>0</xsl:text>
          </xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="@* | *[@css:display = 'list-item']"/>
    </fo:list-block>
  </xsl:template>


  <xsl:template match="*[@css:display = 'list-item']" mode="list-style-position">
    <xsl:variable name="list-style-position"
      select="ancestor-or-self::*[@css:list-style-position != 'inherit'][1]/@css:list-style-position"/>
    <xsl:choose>
      <xsl:when test="string-length($list-style-position) = 0">
        <xsl:text>outside</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$list-style-position"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template match="*[@css:display = 'list-item']" mode="list-style-type">
    <xsl:variable name="att"
      select="ancestor-or-self::*[@css:list-style-type != 'inherit' or @css:list-style-image != 'inherit'][1]"/>
    <xsl:choose>
      <xsl:when test="$att/@css:list-style-image">
        <xsl:text>image</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="list-style-type" select="$att/@css:list-style-type"/>
        <xsl:choose>
          <xsl:when test="string-length($list-style-type) = 0">
            <xsl:text>disc</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$list-style-type"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template match="*[@css:display = 'list-item']" mode="list-label-width">
    <xsl:param name="list-type"/>

    <xsl:choose>
      <xsl:when test="$list-type = 'image'">
        <xsl:value-of
          select="ancestor-or-self::*[@css:list-label-width != 'inherit'][1]/@css:list-label-width"/>
      </xsl:when>
      <xsl:when
        test="$list-type = 'disc' or $list-type = 'circle' or $list-type = 'square' or $list-type = 'box' or $list-type = 'check' or $list-type = 'diamond' or $list-type = 'hyphen' or $list-type = 'dash'">
        <xsl:text>1em</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="get-num-list-label-width">
          <xsl:with-param name="items" select="count(../*[@css:display = 'list-item'])"/>
          <xsl:with-param name="list-type" select="$list-type"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table' and not(*)]" priority="1">
    <fo:block>
      <xsl:apply-templates select="@*"/>
    </fo:block>
  </xsl:template>


  <xsl:template
    match="*[@css:display = 'table' and *[@css:display] and not(*[@css:display = 'table-row-group'])]"
    priority="1"/>
  <xsl:template match="*[@css:display = 'table-header-group' and not(*)]" priority="1"/>
  <xsl:template match="*[@css:display = 'table-footer-group' and not(*)]" priority="1"/>


  <xsl:template
    match="*[@css:display = 'table' and *[@css:display = 'table-caption'] and not(*[@css:display = 'table-row'])]">
    <fo:table-and-caption>
      <xsl:apply-templates select="*[@css:display = 'table-caption']/@css:caption-side"/>
      <xsl:apply-templates select="@*[starts-with(name(), 'css:margin-')]"/>
      <xsl:apply-templates select="*[@css:display = 'table-caption']"/>
      <fo:table margin-left="0pt" margin-right="0pt">
        <xsl:apply-templates select="@*[not(starts-with(name(), 'css:margin-'))]"/>
        <xsl:apply-templates select="*[@css:display = 'table-column']" mode="layout"/>
        <xsl:apply-templates select="*[@css:display = 'table-column']"/>
        <xsl:apply-templates select="*[@css:display = 'table-header-group']"/>
        <xsl:apply-templates select="*[@css:display = 'table-footer-group']"/>
        <xsl:apply-templates select="*[@css:display = 'table-row-group']"/>
      </fo:table>
    </fo:table-and-caption>
  </xsl:template>


  <xsl:template
    match="*[@css:display = 'table' and       not(*[@css:display = 'table-caption']) and not(*[@css:display = 'table-row'])]">
    <fo:table margin-left="0pt" margin-right="0pt">
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="*[@css:display = 'table-column']" mode="layout"/>
      <xsl:apply-templates select="*[@css:display = 'table-column']"/>
      <xsl:apply-templates select="*[@css:display = 'table-header-group']"/>
      <xsl:apply-templates select="*[@css:display = 'table-footer-group']"/>
      <xsl:apply-templates select="*[@css:display = 'table-row-group']"/>
    </fo:table>
  </xsl:template>


  <xsl:template
    match="*[@css:display = 'table' and *[@css:display = 'table-caption'] and *[@css:display = 'table-row']]">
    <fo:table-and-caption>
      <xsl:apply-templates select="*[@css:display = 'table-caption']/@css:caption-side"/>
      <xsl:apply-templates select="@*[starts-with(name(), 'css:margin-')]"/>
      <xsl:apply-templates select="*[@css:display = 'table-caption']"/>
      <fo:table margin-left="0pt" margin-right="0pt">
        <xsl:apply-templates select="@*[not(starts-with(name(), 'css:margin-'))]"/>
        <xsl:apply-templates select="*[@css:display = 'table-column']" mode="layout"/>
        <xsl:apply-templates select="*[@css:display = 'table-column']"/>
        <fo:table-body>
          <xsl:apply-templates select="*[@css:display = 'table-row']"/>
        </fo:table-body>
      </fo:table>
    </fo:table-and-caption>
  </xsl:template>


  <xsl:template
    match="*[@css:display = 'table' and not(*[@css:display = 'table-caption']) and *[@css:display = 'table-row']]">
    <fo:table margin-left="0pt" margin-right="0pt">
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="*[@css:display = 'table-column']" mode="layout"/>
      <xsl:apply-templates select="*[@css:display = 'table-column']"/>
      <fo:table-body>
        <xsl:apply-templates select="*[@css:display = 'table-row']"/>
      </fo:table-body>
    </fo:table>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-caption']">
    <fo:table-caption margin-left="0pt" margin-right="0pt">
      <xsl:apply-templates select="@*[name() != 'css:caption-side']"/>
      <fo:block>
        <xsl:apply-templates select="node()"/>
      </fo:block>
    </fo:table-caption>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-caption']/@css:caption-side[. = 'bottom']"
    priority="1">
    <xsl:attribute name="caption-side">after</xsl:attribute>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-caption']/@css:caption-side[. = 'left']"
    priority="1">
    <xsl:attribute name="caption-side">start</xsl:attribute>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-caption']/@css:caption-side[. = 'right']"
    priority="1">
    <xsl:attribute name="caption-side">end</xsl:attribute>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-caption']/@css:caption-side[. = 'top']" priority="1">
    <xsl:attribute name="caption-side">before</xsl:attribute>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-cell']">
    <fo:table-cell>
      <xsl:apply-templates select="@*"/>
      <fo:block>
        <xsl:apply-templates select="node()"/>
      </fo:block>
    </fo:table-cell>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-cell']/@css:colspan" priority="1">
    <xsl:attribute name="number-columns-spanned">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-cell']/@css:rowspan" priority="1">
    <xsl:attribute name="number-rows-spanned">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-cell']/@css:vertical-align[. = 'bottom']"
    priority="1">
    <xsl:attribute name="display-align">after</xsl:attribute>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-cell']/@css:vertical-align[. = 'middle']"
    priority="1">
    <xsl:attribute name="display-align">center</xsl:attribute>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-cell']/@css:vertical-align[. = 'top']" priority="1">
    <xsl:attribute name="display-align">before</xsl:attribute>
  </xsl:template>


  <!-- Unsupported vertical-align values for table-cells. -->

  <xsl:template
    match="*[@css:display = 'table-cell']/@css:vertical-align[. != 'top' and . != 'bottom' and . != 'middle']"
    priority="1"/>


  <xsl:template match="*[@css:display = 'table-column']">
    <fo:table-column>
      <xsl:apply-templates select="@*"/>
    </fo:table-column>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-column' and contains(@css:width, '*')]"
    mode="layout">
    <xsl:attribute name="table-layout">fixed</xsl:attribute>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-column']/@css:span" priority="1">
    <xsl:attribute name="number-columns-repeated">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <!--
    The inheritance mechanism of the table normalizer might have propagated
    these.
  -->

  <xsl:template match="*[@css:display = 'table-column']/@css:text-align" priority="1"/>
  <xsl:template match="*[@css:display = 'table-column']/@css:vertical-align" priority="1"/>
  <xsl:template match="*[@css:display = 'table-footer-group']/@css:text-align" priority="1"/>
  <xsl:template match="*[@css:display = 'table-footer-group']/@css:vertical-align" priority="1"/>
  <xsl:template match="*[@css:display = 'table-header-group']/@css:text-align" priority="1"/>
  <xsl:template match="*[@css:display = 'table-header-group']/@css:vertical-align" priority="1"/>
  <xsl:template match="*[@css:display = 'table-row']/@css:text-align" priority="1"/>
  <xsl:template match="*[@css:display = 'table-row']/@css:vertical-align" priority="1"/>
  <xsl:template match="*[@css:display = 'table-row-group']/@css:text-align" priority="1"/>
  <xsl:template match="*[@css:display = 'table-row-group']/@css:vertical-align" priority="1"/>


  <xsl:template match="*[@css:display = 'table-column']/@css:width" priority="1">
    <xsl:attribute name="column-width">
      <xsl:choose>
        <xsl:when test="starts-with(., '*')">
          <xsl:value-of
            select="concat('proportional-column-width(1)', substring-after(., '*'))"/>
        </xsl:when>
        <xsl:when test="contains(., '*')">
          <xsl:value-of
            select="concat('proportional-column-width(', substring-before(., '*'), ')', substring-after(., '*'))"/>
        </xsl:when>
        <xsl:when test="contains(., 'pcw')">
          <xsl:value-of
            select="concat('proportional-column-width(', substring-before(., 'pcw'), ')', substring-after(., 'pcw'))"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="."/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-footer-group']">
    <fo:table-footer>
      <xsl:apply-templates select="@* | node()"/>
    </fo:table-footer>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-header-group']">
    <fo:table-header>
      <xsl:apply-templates select="@* | node()"/>
    </fo:table-header>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-row']">
    <fo:table-row>
      <xsl:apply-templates select="@* | node()"/>
    </fo:table-row>
  </xsl:template>


  <xsl:template match="*[@css:display = 'table-row-group']">
    <fo:table-body>
      <xsl:apply-templates select="@* | node()"/>
    </fo:table-body>
  </xsl:template>


  <xsl:template
    match="*[@css:display = 'block']/@css:margin-bottom[. != 'auto'] | *[@css:display = 'list-item']/@css:margin-bottom[. != 'auto'] | *[@css:display = 'table']/@css:margin-bottom[. != 'auto']"
    priority="1">
    <xsl:attribute name="space-after">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template
    match="*[@css:display = 'block']/@css:margin-top[. != 'auto'] | *[@css:display = 'list-item']/@css:margin-top[. != 'auto'] | *[@css:display = 'table']/@css:margin-top[. != 'auto']"
    priority="1">
    <xsl:attribute name="space-before">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="*[@css:display = 'leader']">
    <fo:leader>
      <xsl:apply-templates select="@* | node()"/>
    </fo:leader>
  </xsl:template>


  <xsl:template match="css:external-link">
    <fo:basic-link external-destination="{concat('url(', @target, ')')}">
      <xsl:apply-templates/>
    </fo:basic-link>
  </xsl:template>


  <xsl:template match="css:footnote">
    <fo:footnote>
      <xsl:apply-templates select="css:footnote-reference/*"/>
      <fo:footnote-body>
        <xsl:apply-templates select="css:footnote-body/*"/>
      </fo:footnote-body>
    </fo:footnote>
  </xsl:template>


  <xsl:template match="css:internal-link">
    <fo:basic-link internal-destination="{@target}">
      <xsl:apply-templates/>
    </fo:basic-link>
  </xsl:template>


  <xsl:template match="@css:anchor" priority="1">
    <xsl:attribute name="id">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="@css:column-span" priority="1">
    <xsl:attribute name="span">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="@css:vlink" priority="1"/>


  <xsl:template match="@css:white-space[. = 'wrap-nocollapse' or . = 'pre-wrap']" priority="1">
    <xsl:attribute name="linefeed-treatment">preserve</xsl:attribute>
    <xsl:attribute name="white-space-collapse">false</xsl:attribute>
    <xsl:attribute name="white-space-treatment">preserve</xsl:attribute>
    <xsl:attribute name="wrap-option">wrap</xsl:attribute>
  </xsl:template>


  <!--
    The following two templates are temporarily in the RenderX namespace.
  -->

  <xsl:template match="css:change-bar-begin">
    <fo:change-bar-begin>
      <xsl:copy-of select="@*"/>
    </fo:change-bar-begin>
  </xsl:template>


  <xsl:template match="css:change-bar-end">
    <fo:change-bar-end>
      <xsl:copy-of select="@*"/>
    </fo:change-bar-end>
  </xsl:template>


  <xsl:template match="css:external[@css:href != '']">
    <fo:external-graphic src="{@css:href}"/>
  </xsl:template>


  <xsl:template match="css:first-line">
    <fo:initial-property-set>
      <xsl:apply-templates select="@*"/>
    </fo:initial-property-set>
  </xsl:template>


  <xsl:template match="css:float">
    <fo:float>
      <xsl:apply-templates select="@*"/>
      <fo:block>
        <xsl:apply-templates select="node()"/>
      </fo:block>
    </fo:float>
  </xsl:template>


  <xsl:template match="css:fo-marker">
    <fo:marker marker-class-name="{@css:name}">
      <xsl:apply-templates select="node()"/>
    </fo:marker>
  </xsl:template>


  <xsl:template match="css:last-page-mark">
    <fo:block id="last-page"/>
  </xsl:template>


  <xsl:template match="css:newline">
    <fo:block/>
  </xsl:template>


  <xsl:template match="css:page-number">
    <fo:page-number/>
  </xsl:template>


  <xsl:template match="css:page-number" mode="page-number-setup">
    <xsl:apply-templates select="@css:format" mode="page-number-setup"/>
    <xsl:apply-templates select="@css:letter-value" mode="page-number-setup"/>
  </xsl:template>


  <xsl:template match="css:page-number/@css:format" mode="page-number-setup">
    <xsl:attribute name="format">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="css:page-number/@css:letter-value" mode="page-number-setup">
    <xsl:attribute name="letter-value">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>


  <xsl:template match="css:pages-total">
    <fo:page-number-citation ref-id="last-page"/>
  </xsl:template>


  <xsl:template match="css:page-ref">
    <fo:page-number-citation ref-id="{@css:ref-id}"/>
  </xsl:template>


  <xsl:template match="css:retrieve-fo-marker">
    <fo:retrieve-marker retrieve-class-name="{@css:name}">
      <xsl:apply-templates select="@*"/>
    </fo:retrieve-marker>
  </xsl:template>


  <xsl:template match="css:save-counters"/>


  <xsl:template match="css:save-counters/css:counter">
    <xsl:param name="style"/>
    <xsl:variable name="format">
      <xsl:call-template name="get-num-format">
        <xsl:with-param name="list-type" select="$style"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:number value="@css:value" format="{$format}"/>
  </xsl:template>


  <xsl:template match="css:save-counters/css:counters">
    <xsl:param name="separator"/>
    <xsl:param name="style"/>
    <xsl:variable name="format">
      <xsl:call-template name="get-num-format">
        <xsl:with-param name="list-type" select="$style"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:apply-templates select="css:level">
      <xsl:with-param name="format" select="$format"/>
      <xsl:with-param name="separator" select="$separator"/>
    </xsl:apply-templates>
  </xsl:template>


  <xsl:template match="css:save-counters/css:counters/css:level[not(preceding-sibling::css:level)]">
    <xsl:param name="format"/>
    <xsl:number value="@css:value" format="{$format}"/>
  </xsl:template>


  <xsl:template match="css:save-counters/css:counters/css:level[preceding-sibling::css:level]">
    <xsl:param name="format"/>
    <xsl:param name="separator"/>
    <xsl:value-of select="$separator"/>
    <xsl:number value="@css:value" format="{$format}"/>
  </xsl:template>


  <xsl:template match="css:target-counter[@css:name != 'page']">
    <xsl:variable name="name" select="@css:name"/>
    <xsl:apply-templates select="id(@css:ref-id)/css:save-counters/css:counter[@css:name = $name]">
      <xsl:with-param name="style" select="@css:style"/>
    </xsl:apply-templates>
  </xsl:template>


  <xsl:template match="css:target-counter[@css:name = 'page']">
    <fo:page-number-citation ref-id="{@css:ref-id}"/>
  </xsl:template>


  <xsl:template match="css:target-counters">
    <xsl:variable name="name" select="@css:name"/>
    <xsl:apply-templates select="id(@css:ref-id)/css:save-counters/css:counters[@css:name = $name]">
      <xsl:with-param name="separator" select="@css:separator"/>
      <xsl:with-param name="style" select="@css:style"/>
    </xsl:apply-templates>
  </xsl:template>


  <xsl:template match="css:target-text">
    <xsl:apply-templates select="id(@css:ref-id)" mode="target-text">
      <xsl:with-param name="content" select="@css:content"/>
    </xsl:apply-templates>
  </xsl:template>


  <xsl:template match="*" mode="target-text">
    <xsl:param name="content"/>
    <xsl:if test="$content = 'content-first-letter'">
      <xsl:value-of
        select="substring(string(node()[namespace-uri(.) != 'http://www.w3.org/1998/CSS']), 1, 1)"/>
    </xsl:if>
    <xsl:if test="$content = 'contents' or $content = 'content-before'">
      <xsl:value-of select="string(css:before)"/>
    </xsl:if>
    <xsl:if test="$content = 'contents' or $content = 'content-element'">
      <xsl:value-of select="string(node()[namespace-uri(.) != 'http://www.w3.org/1998/CSS'])"/>
    </xsl:if>
    <xsl:if test="$content = 'contents' or $content = 'content-after'">
      <xsl:value-of select="string(css:after)"/>
    </xsl:if>
  </xsl:template>


  <!-- Named templates. -->

  <xsl:template name="get-num-format">
    <xsl:param name="list-type"/>

    <xsl:choose>
      <xsl:when test="$list-type = 'lower-alpha' or $list-type = 'lower-latin'">
        <xsl:text>a</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'lower-roman'">
        <xsl:text>i</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'upper-alpha' or $list-type = 'upper-latin'">
        <xsl:text>A</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'upper-roman'">
        <xsl:text>I</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'arabic-indic'">
        <xsl:text>&#1633;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'bengali'">
        <xsl:text>&#2535;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'binary'">
        <xsl:text>1</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'burmese'">
        <xsl:text>&#4161;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'cambodian'">
        <xsl:text>&#6112;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'cjk-decimal'">
        <xsl:text>&#19968;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'decimal'">
        <xsl:text>1</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'devanagari'">
        <xsl:text>&#2407;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'eastern-nagari'">
        <xsl:text>&#2535;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'fullwidth-decimal'">
        <xsl:text>&#65296;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'gujarati'">
        <xsl:text>&#2791;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'gurmukhi'">
        <xsl:text>&#2663;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'kannada'">
        <xsl:text>&#3303;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'khmer'">
        <xsl:text>&#6113;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'lower-hexadecimal'">
        <xsl:text>1</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'lao'">
        <xsl:text>&#3793;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'lepcha'">
        <xsl:text>&#7233;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'malayalam'">
        <xsl:text>&#3431;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'marathi'">
        <xsl:text>&#2407;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'mongolian'">
        <xsl:text>&#6161;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'myanmar'">
        <xsl:text>&#4161;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'new-base-60'">
        <xsl:text>1</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'octal'">
        <xsl:text>1</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'oriya'">
        <xsl:text>&#2919;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'persian'">
        <xsl:text>&#1777;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'super-decimal'">
        <xsl:text>&#185;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'tamil'">
        <xsl:text>&#3047;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'telugu'">
        <xsl:text>&#3175;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'tibetan'">
        <xsl:text>&#3873;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'thai'">
        <xsl:text>&#3665;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'upper-hexadecimal'">
        <xsl:text>1</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>1</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template name="get-num-list-label-width">
    <xsl:param name="items"/>
    <xsl:param name="list-type"/>

    <xsl:choose>
      <xsl:when
        test="$list-type='lower-alpha' or $list-type = 'lower-latin' or $list-type = 'lower-greek'">
        <xsl:choose>
          <xsl:when test="$items &lt; 27">
            <xsl:text>1.5em</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>2em</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$list-type='upper-alpha' or $list-type = 'upper-latin'">
        <xsl:choose>
          <xsl:when test="$items&lt;27">
            <xsl:text>1.5em</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>2.5em</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$list-type='lower-roman'">
        <xsl:choose>
          <xsl:when test="$items&lt;7">
            <xsl:text>1.5em</xsl:text>
          </xsl:when>
          <xsl:when test="$items&lt;17">
            <xsl:text>2em</xsl:text>
          </xsl:when>
          <xsl:when test="$items&lt;27">
            <xsl:text>2.5em</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>3.5em</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$list-type='upper-roman'">
        <xsl:choose>
          <xsl:when test="$items&lt;7">
            <xsl:text>2em</xsl:text>
          </xsl:when>
          <xsl:when test="$items&lt;17">
            <xsl:text>2.5em</xsl:text>
          </xsl:when>
          <xsl:when test="$items&lt;27">
            <xsl:text>3em</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>4em</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="$items&lt;10">
            <xsl:text>1.5em</xsl:text>
          </xsl:when>
          <xsl:when test="$items&lt;100">
            <xsl:text>2em</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>2.5em</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template name="list-label">
    <xsl:param name="list-type"/>

    <xsl:choose>
      <xsl:when test="$list-type = 'image'">
        <xsl:variable name="src"
          select="ancestor-or-self::*[@css:list-style-image != 'inherit'][1]/@css:list-style-image"/>
        <xsl:if test="$src != ''">
          <fo:external-graphic>
            <xsl:attribute name="src">
              <xsl:value-of select="$src"/>
            </xsl:attribute>
          </fo:external-graphic>
        </xsl:if>
      </xsl:when>
      <xsl:when test="$list-type = 'box'">
        <xsl:text>&#9633;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'check'">
        <xsl:text>&#10003;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'circle'">
        <xsl:text>&#9702;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'diamond'">
        <xsl:text>&#9830;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'disc'">
        <xsl:text>&#8226;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'hyphen'">
        <xsl:text>&#8211;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'dash'">
        <xsl:text>&#8212;</xsl:text>
      </xsl:when>
      <xsl:when test="$list-type = 'square'">
        <xsl:text>&#9632;</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="format">
          <xsl:call-template name="get-num-format">
            <xsl:with-param name="list-type" select="$list-type"/>
          </xsl:call-template>
        </xsl:variable>
        <xsl:number format="{$format}"
          value="count(preceding-sibling::*[@css:display = 'list-item']) + 1"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:transform>
