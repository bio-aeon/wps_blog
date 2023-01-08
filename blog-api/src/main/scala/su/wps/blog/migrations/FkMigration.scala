package su.wps.blog.migrations

import su.wps.pgmigrations._

trait FkMigration extends Migration {

  val tableName: String
  val refTableName: String
  val fieldName: String
  val refFieldName: String

  /**
    * Name of the fk and corresponding index
    * prefixes ("fk_" & "idx_") will be added automatically
    */
  val referenceName: String
  val options: List[ForeignKeyOption] = List(OnDelete(Cascade), OnUpdate(Restrict))

  protected lazy val fkRefName = s"fk_$referenceName"
  protected lazy val idxRefName = s"idx_$referenceName"
  protected lazy val optionsWithName = options.::(Name(fkRefName))

  protected def addFk = {
    addForeignKey(
      on(tableName -> fieldName),
      references(refTableName -> refFieldName),
      optionsWithName: _*
    )

    if (!addingForeignKeyConstraintCreatesIndex) {
      addIndex(tableName, fieldName, Name(idxRefName))
    }
  }

  protected def dropFk = {
    removeForeignKey(
      on(tableName -> fieldName),
      references(refTableName -> refFieldName),
      Name(fkRefName)
    )

    if (!addingForeignKeyConstraintCreatesIndex) {
      removeIndex(tableName, fieldName, Name(idxRefName))
    }
  }

}

trait AddFkMigration extends FkMigration {

  def up(): Unit = addFk

  def down(): Unit = dropFk

}

trait DropFkMigration extends FkMigration {

  def up(): Unit = dropFk

  def down(): Unit = addFk

}
