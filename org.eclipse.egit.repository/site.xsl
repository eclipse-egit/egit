<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html" indent="yes" />
	<xsl:template match="/site">
		<table cellspacing="2" cellpadding="0" border="0">
			<xsl:if test="count(feature)>0">
				<tr style="background-color:#DDDDDD">
					<th class="column-header">Feature</th>
					<th class="column-header">Version</th>
					<th class="column-header">Feature Categor(ies)</th>
				</tr>
				<xsl:for-each select="feature">
					<xsl:sort select="@id" />
					<xsl:variable name="rowCol">
						<xsl:if test="position() mod 2 = 1">
							#EEEEEE
						</xsl:if>
						<xsl:if test="position() mod 2 = 0">
							#FFFFFF
						</xsl:if>
					</xsl:variable>

					<tr style="background-color:{$rowCol}">
						<td class="rowLine">
							<a href="{@url}" style="font-size:x-small">
								<xsl:value-of select="@id" />
							</a>
						</td>
						<td>
							<span style="font-size:x-small">
								<xsl:value-of select="@version" />
							</span>
						</td>
						<td>
							<xsl:choose>
								<xsl:when test="category">
									<xsl:for-each select="category">
										<span style="font-size:x-small">
											|
											<xsl:value-of select="@name" />
										</span>
									</xsl:for-each>
								</xsl:when>
							</xsl:choose>
						</td>
					</tr>
				</xsl:for-each>
			</xsl:if>

			<tr style="background-color:#DDDDDD">
				<th class="column-header">Feature</th>
				<th class="column-header">Version</th>
				<th class="column-header">Feature Categor(ies)</th>
			</tr>
			<tr style="background-color:#EEEEEE">
				<td class="rowLine" colspan="1">
					<a href="artifacts.jar" style="font-size:x-small">artifacts.jar</a>
					::
					<a href="content.jar" style="font-size:x-small">content.jar</a>
				</td>
				<td class="rowLine" colspan="1">
					<a href="plugins/" style="font-size:x-small">plugins</a>
				<xsl:if test="count(feature)>0">
					::
					<a href="features/" style="font-size:x-small">features</a>
				</xsl:if>
				</td>
				<td class="rowLine" colspan="1">
				</td>
			</tr>
		</table>
		<br />
	</xsl:template>
</xsl:stylesheet>